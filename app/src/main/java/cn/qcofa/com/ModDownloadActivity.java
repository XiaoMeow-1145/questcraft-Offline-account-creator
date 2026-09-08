package cn.qcofa.com;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import cn.qcofa.com.data.ModInfo;
import cn.qcofa.com.data.ModRepository;
import cn.qcofa.com.data.ModVersionFile;

public class ModDownloadActivity extends AppCompatActivity {

    private TextInputEditText searchInput;
    private Button searchBtn;
    private LinearLayout modListContainer;
    private TextView resultCountText;
    private ModRepository modRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mod_download);

        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Fabric 模组下载");
        }

        modRepository = new ModRepository();

        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        modListContainer = findViewById(R.id.modListContainer);
        resultCountText = findViewById(R.id.resultCountText);

        // 默认搜索热门模组
        searchInput.setText("");

        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            searchBtn.setEnabled(false);
            searchBtn.setText("搜索中...");
            new SearchModsTask().execute(query);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class SearchModsTask extends AsyncTask<String, Void, ModRepository.ModResult> {

        @Override
        protected ModRepository.ModResult doInBackground(String... params) {
            return modRepository.searchMods(params[0], 0);
        }

        @Override
        protected void onPostExecute(ModRepository.ModResult result) {
            searchBtn.setEnabled(true);
            searchBtn.setText("搜索");
            modListContainer.removeAllViews();

            if (result == null || result.mods.isEmpty()) {
                resultCountText.setText("未找到相关模组");
                return;
            }

            resultCountText.setText("找到 " + result.mods.size() + " 个模组" +
                    (result.totalHits > 0 ? " (共 " + result.totalHits + " 个)" : ""));

            for (ModInfo mod : result.mods) {
                addModCard(mod);
            }
        }
    }

    private void addModCard(ModInfo mod) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.mod_list_item, null);

        // 设置模组信息
        TextView titleText = cardView.findViewById(R.id.modTitle);
        TextView descText = cardView.findViewById(R.id.modDescription);
        TextView downloadsText = cardView.findViewById(R.id.modDownloads);
        MaterialButton downloadBtn = cardView.findViewById(R.id.downloadBtn);
        ImageView modIcon = cardView.findViewById(R.id.modIcon);
        ProgressBar loadingSpinner = cardView.findViewById(R.id.downloadLoading);

        titleText.setText(mod.title);
        descText.setText(mod.description.length() > 100 ? mod.description.substring(0, 100) + "..." : mod.description);
        downloadsText.setText("下载量: " + formatDownloads(mod.downloads));

        // 加载图标
        if (mod.iconUrl != null && !mod.iconUrl.isEmpty()) {
            new LoadIconTask(modIcon).execute(mod.iconUrl);
        }

        // 下载按钮
        downloadBtn.setOnClickListener(v -> {
            downloadBtn.setVisibility(View.GONE);
            loadingSpinner.setVisibility(View.VISIBLE);
            new DownloadModTask(mod.title, downloadBtn, loadingSpinner).execute(mod.projectId);
        });

        // 添加到列表的LinearLayout包装
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        wrapper.addView(cardView);

        // 添加分隔线
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(getResources().getColor(R.color.outline_variant));

        modListContainer.addView(wrapper);
        modListContainer.addView(divider);
    }

    private String formatDownloads(long count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        }
        return String.valueOf(count);
    }

    private class DownloadModTask extends AsyncTask<String, Void, ModVersionFile> {

        private final String modName;
        private final Button downloadBtn;
        private final ProgressBar loadingSpinner;

        DownloadModTask(String modName, Button downloadBtn, ProgressBar loadingSpinner) {
            this.modName = modName;
            this.downloadBtn = downloadBtn;
            this.loadingSpinner = loadingSpinner;
        }

        @Override
        protected ModVersionFile doInBackground(String... params) {
            return modRepository.getLatestVersionFile(params[0]);
        }

        @Override
        protected void onPostExecute(ModVersionFile versionFile) {
            loadingSpinner.setVisibility(View.GONE);
            downloadBtn.setVisibility(View.VISIBLE);

            if (versionFile == null) {
                Toast.makeText(ModDownloadActivity.this,
                        "获取下载链接失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用 DownloadManager 下载到公共 Downloads 目录
            try {
                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager == null) {
                    Toast.makeText(ModDownloadActivity.this, "下载服务不可用", Toast.LENGTH_SHORT).show();
                    return;
                }

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(versionFile.url));
                request.setTitle(modName);
                request.setDescription("正在下载 Fabric 模组");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "QcofA_Mods/" + versionFile.filename);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);

                long downloadId = downloadManager.enqueue(request);
                downloadBtn.setText("已加入下载队列");
                downloadBtn.setEnabled(false);

                Toast.makeText(ModDownloadActivity.this,
                        "已开始下载: " + modName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(ModDownloadActivity.this,
                        "下载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class LoadIconTask extends AsyncTask<String, Void, android.graphics.Bitmap> {

        private final ImageView imageView;

        LoadIconTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected android.graphics.Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "QcofA/2.0 (Android)");
                InputStream is = conn.getInputStream();
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();
                return bitmap;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(android.graphics.Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}