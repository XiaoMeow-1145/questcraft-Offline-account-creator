package cn.qcofa.com;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class AboutFragment extends Fragment {

    private static final String GITHUB_URL = "https://github.com/XiaoMeow-114514/questcraft-Offline-account-creator";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置版本号
        TextView versionTextView = view.findViewById(R.id.tv_version);
        String version = getAppVersion();
        versionTextView.setText(version);

        // 设置源码链接
        TextView sourceUrlTextView = view.findViewById(R.id.tv_source_url);
        sourceUrlTextView.setText(GITHUB_URL);

        // 点击卡片复制链接
        MaterialCardView cardSource = view.findViewById(R.id.cardSource);
        cardSource.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();

            copyToClipboard(GITHUB_URL);
        });

        // 点击版本号也可以打开浏览器
        versionTextView.setOnClickListener(v -> openInBrowser(GITHUB_URL));
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            return "v" + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "未知版本";
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("GitHub 链接", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    private void openInBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "没有可用的浏览器", Toast.LENGTH_SHORT).show();
        }
    }
}
