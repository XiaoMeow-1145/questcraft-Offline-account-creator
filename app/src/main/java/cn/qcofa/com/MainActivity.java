package cn.qcofa.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "QcofA";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private EditText usernameInput;
    private EditText customUuidInput;
    private Spinner userTypeSpinner;
    private TextView uuidDisplay;
    private EditText ramValueInput;
    private CheckBox legalCheck, devModsCheck, customRamCheck, demoModeCheck;
    private Button manualInstallJreBtn;
    private Button viewAccountsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在setContentView之前应用保存的主题风格
        applyThemeStyle();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        initViews();

        // 加载并显示当前账号信息
        loadAndShowCurrentAccount();

        // 请求必要的权限
        requestPermissions();

        // 设置按钮点击事件
        setupClickListeners();
    }

    private void applyThemeStyle() {
        SharedPreferences prefs = getSharedPreferences("theme_style", MODE_PRIVATE);
        String style = prefs.getString("style", "Material");
        if ("Miuix".equals(style)) {
            setTheme(R.style.Theme_QcofA_Miuix);
        } else {
            setTheme(R.style.Theme_QcofA_Material);
        }
    }

    private void initViews() {
        usernameInput = findViewById(R.id.usernameInput);
        customUuidInput = findViewById(R.id.customUuidInput);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        uuidDisplay = findViewById(R.id.uuidDisplay);
        ramValueInput = findViewById(R.id.ramValueInput);
        
        legalCheck = findViewById(R.id.legalCheck);
        devModsCheck = findViewById(R.id.devModsCheck);
        customRamCheck = findViewById(R.id.customRamCheck);
        demoModeCheck = findViewById(R.id.demoModeCheck);
        manualInstallJreBtn = findViewById(R.id.manualInstallJreBtn);
        viewAccountsBtn = findViewById(R.id.viewAccountsBtn);

        // 设置用户类型选择器
        setupUserTypeSpinner();

        // 设置默认值
        ramValueInput.setText("2048");
        // 不再自动设置用户名，让用户自行输入
    }

    private void setupUserTypeSpinner() {
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                this, 
                R.array.user_types_array, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        Button createAccountBtn = findViewById(R.id.createAccountBtn);
        createAccountBtn.setOnClickListener(v -> createAccountFile());

        Button saveConfigBtn = findViewById(R.id.saveConfigBtn);
        saveConfigBtn.setOnClickListener(v -> saveConfigFiles());

        manualInstallJreBtn.setOnClickListener(v -> showJreInstallationDialog());
        
        viewAccountsBtn.setOnClickListener(v -> showAccountsList());

        // 设置折叠/展开功能的点击事件
        LinearLayout expandableSectionHeader = findViewById(R.id.expandableSectionHeader);
        TextView expandIndicator = findViewById(R.id.expandIndicator);
        LinearLayout expandableSection = findViewById(R.id.expandableSection);
        
        expandableSectionHeader.setOnClickListener(v -> {
            boolean isExpanded = expandableSection.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                expandableSection.setVisibility(View.GONE);
                expandIndicator.setText("▶");
            } else {
                expandableSection.setVisibility(View.VISIBLE);
                expandIndicator.setText("▼");
            }
        });

        // 为用户名输入框添加文本变化监听器，自动触发UUID生成
        usernameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                autoGenerateUUIDIfNeeded();
            }
        });
        
        // 为自定义UUID输入框添加文本变化监听器
        customUuidInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 当用户输入自定义UUID时，更新显示
                String customUuid = customUuidInput.getText().toString().trim();
                if (!customUuid.isEmpty()) {
                    uuidDisplay.setText("UUID: " + customUuid);
                } else {
                    // 如果清空了自定义UUID，则重新自动生成
                    autoGenerateUUIDIfNeeded();
                }
            }
        });
    }

    private void autoGenerateUUIDIfNeeded() {
        String username = usernameInput.getText().toString().trim();
        if (!username.isEmpty()) {
            String generatedUUID = generateUUID(username);
            uuidDisplay.setText("UUID: " + generatedUUID);
        } else {
            uuidDisplay.setText("UUID: ");
        }
    }

    private String generateUUID(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            
            // 格式化为正确的UUID格式：8-4-4-4-12
            String formattedUUID = String.format("%s-%s-%s-%s-%s",
                hashtext.substring(0, 8),
                hashtext.substring(8, 12),
                hashtext.substring(12, 16),
                hashtext.substring(16, 20),
                hashtext.substring(20, 32));
            
            return formattedUUID;
        } catch (Exception e) {
            Log.e("QcofA", "MD5哈希计算失败", e);
            return UUID.randomUUID().toString();
        }
    }

    private String extractUUIDFromDisplay() {
        String uuidText = uuidDisplay.getText().toString();
        if (uuidText.startsWith("UUID: ")) {
            return uuidText.substring(6); // 移除 "UUID: " 前缀
        }
        return uuidText;
    }

    private void createAccountFile() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(this, "请先生成UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建存储目录
            File storageDir = new File(getExternalFilesDir(null), "questcraft_accounts");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
                Log.d(TAG, "创建目录: " + storageDir.getAbsolutePath());
            }

            // 创建账号JSON文件
            File jsonFile = new File(storageDir, uuid + ".json");
            JSONObject accountJson = new JSONObject();
            accountJson.put("accessToken", "0");
            accountJson.put("expiresOn", 0);
            accountJson.put("isDemoMode", demoModeCheck.isChecked());
            accountJson.put("userType", getUserType());
            accountJson.put("username", username);
            accountJson.put("uuid", uuid);

            // 写入文件
            FileWriter writer = new FileWriter(jsonFile);
            writer.write(accountJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(this, "账号文件创建成功: " + jsonFile.getName(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "账号文件创建成功: " + jsonFile.getAbsolutePath());
            
            // 同时更新launcher.conf文件，将新创建的账号添加到配置文件中
            updateLauncherConf(username, uuid);

        } catch (Exception e) {
            Log.e(TAG, "创建账号文件失败", e);
            Toast.makeText(this, "创建账号文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateLauncherConf(String username, String uuid) {
        try {
            // 创建launcher.conf文件
            File storageDir = new File(getExternalFilesDir(null), "questcraft_accounts");
            File confFile = new File(storageDir, "launcher.conf");

            JSONObject confJson = new JSONObject();
            confJson.put("acceptedLegal", legalCheck.isChecked());
            confJson.put("setDevMods", devModsCheck.isChecked());
            confJson.put("setCustomRAM", customRamCheck.isChecked());
            confJson.put("customRAMValue", ramValueInput.getText().toString().isEmpty() ? "2048" : ramValueInput.getText().toString());
            confJson.put("lastSelectedInstance", 0);
            confJson.put("lastSelectedAccount", 0);

            // 创建或更新账号数组
            JSONArray accountsArray = new JSONArray();
            
            // 如果配置文件已存在，加载已有账号并添加新账号
            if (confFile.exists()) {
                String existingContent = readFileToString(confFile);
                JSONObject existingConf = new JSONObject(existingContent);
                
                if (existingConf.has("accounts")) {
                    JSONArray existingAccounts = existingConf.getJSONArray("accounts");
                    // 添加已有的账号
                    for (int i = 0; i < existingAccounts.length(); i++) {
                        JSONObject existingAccount = existingAccounts.getJSONObject(i);
                        // 避免重复添加相同UUID的账号
                        if (!existingAccount.getString("uuid").equals(uuid)) {
                            accountsArray.put(existingAccount);
                        }
                    }
                }
            }
            
            // 添加当前新创建的账号
            JSONObject accountObj = new JSONObject();
            accountObj.put("username", username);
            accountObj.put("uuid", uuid);
            accountsArray.put(accountObj);
            
            confJson.put("accounts", accountsArray);

            // 写入文件
            FileWriter writer = new FileWriter(confFile);
            writer.write(confJson.toString(2)); // 格式化缩进
            writer.close();

            Toast.makeText(this, "配置文件更新成功: " + confFile.getName(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "配置文件更新成功: " + confFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "更新配置文件失败", e);
            Toast.makeText(this, "更新配置文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getUserType() {
        return userTypeSpinner.getSelectedItem().toString();
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString().trim();
    }

    private void saveConfigFiles() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(this, "请先生成UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLauncherConf(username, uuid);
        
        // 保存当前账号信息到SharedPreferences
        saveCurrentAccount(username, uuid);
        
        // 显示当前账号信息
        showCurrentAccountInfo();
    }

    private void saveCurrentAccount(String username, String uuid) {
        SharedPreferences.Editor editor = getSharedPreferences("current_account", MODE_PRIVATE).edit();
        editor.putString("username", username);
        editor.putString("uuid", uuid);
        editor.apply();
    }

    private void loadAndShowCurrentAccount() {
        SharedPreferences prefs = getSharedPreferences("current_account", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String uuid = prefs.getString("uuid", "");
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            usernameInput.setText(username);
            uuidDisplay.setText("UUID: " + uuid);
        }
    }

    private void showJreInstallationDialog() {
        // 创建带有三个按钮的对话框 - 使用Material Design
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("手动安装JRE Runtime");
        builder.setMessage("当您启动游戏过程中无法正常下载和安装JRE提供了两个选项可以给您手动下载和安装JRE");

        // 添加"手动下载安装"按钮
        builder.setPositiveButton("手动下载安装", (dialog, which) -> openJreDownloadPage());
        
        // 添加"本地导出JRE"按钮
        builder.setNeutralButton("本地导出JRE", (dialog, which) -> exportJreToPrivateDirectory());
        
        // 添加"取消"按钮
        builder.setNegativeButton("取消", null);
        
        builder.show();
    }

    private void openJreDownloadPage() {
        try {
            // 打开浏览器跳转到JRE下载页面
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuestCraftPlusPlus/android-openjdk-build-multiarch/releases/tag/jre22-6.0.0"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportJreToPrivateDirectory() {
        try {
            // 获取应用外部私有目录 (/storage/emulated/0/Android/data/cn.qcofa.com/files/)
            File privateDir = getExternalFilesDir("jre_runtime");
            if (privateDir == null) {
                // 如果外部存储不可用，则使用内部存储
                privateDir = new File(getFilesDir(), "jre_runtime");
            }
            
            if (!privateDir.exists()) {
                privateDir.mkdirs();
            }

            // 定义目标文件路径
            File jreZipFile = new File(privateDir, "JRE.zip");

            // 从assets中读取JRE.zip并写入私有目录
            InputStream inputStream = getAssets().open("JRE.zip");
            FileOutputStream outputStream = new FileOutputStream(jreZipFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "JRE已成功导出到目录: " + jreZipFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "导出JRE失败", e);
            Toast.makeText(this, "导出JRE失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAccountsList() {
        try {
            // 读取launcher.conf文件
            File storageDir = new File(getExternalFilesDir(null), "questcraft_accounts");
            File confFile = new File(storageDir, "launcher.conf");
            
            if (!confFile.exists()) {
                Toast.makeText(this, "暂无已创建的账号", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String content = readFileToString(confFile);
            JSONObject confJson = new JSONObject(content);
            
            JSONArray accountsArray = confJson.getJSONArray("accounts");
            
            if (accountsArray.length() == 0) {
                Toast.makeText(this, "暂无已创建的账号", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建自定义视图的对话框
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle("已创建的账号列表");
            
            // 创建列表视图
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(20, 10, 20, 10);
            
            // 为每个账号创建列表项
            for (int i = 0; i < accountsArray.length(); i++) {
                JSONObject account = accountsArray.getJSONObject(i);
                String username = account.getString("username");
                String uuid = account.getString("uuid");
                
                // 创建账号项视图
                View accountItemView = getLayoutInflater().inflate(R.layout.account_list_item, null);
                
                // 设置账号信息
                TextView usernameView = accountItemView.findViewById(R.id.accountUsername);
                TextView uuidView = accountItemView.findViewById(R.id.accountUuid);
                TextView accountTypeLabel = accountItemView.findViewById(R.id.accountTypeLabel);
                
                usernameView.setText("用户名: " + username);
                uuidView.setText("UUID: " + uuid);
                
                // 获取账户类型，如果没有则默认为离线账户
                String accountType = "offline";
                if (account.has("accountType")) {
                    accountType = account.getString("accountType");
                }
                
                // 根据账户类型设置标签
                if ("premium".equals(accountType)) {
                    accountTypeLabel.setText("正版账户");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                } else {
                    accountTypeLabel.setText("离线账户");
                    accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                }
                
                // 设置标签点击事件
                final int accountIndex = i;
                final String currentAccountType = accountType; // 创建final副本
                accountTypeLabel.setOnClickListener(v -> {
                    try {
                        // 切换账户类型
                        String newAccountType = "offline".equals(currentAccountType) ? "premium" : "offline";
                        
                        // 更新JSON中的账户类型
                        accountsArray.getJSONObject(accountIndex).put("accountType", newAccountType);
                        
                        // 保存更新后的JSON到文件
                        confJson.put("accounts", accountsArray);
                        writeStringToFile(confFile, confJson.toString(2));
                        
                        // 更新标签显示
                        if ("premium".equals(newAccountType)) {
                            accountTypeLabel.setText("正版账户");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_success));
                        } else {
                            accountTypeLabel.setText("离线账户");
                            accountTypeLabel.setBackgroundTintList(getResources().getColorStateList(R.color.state_info));
                        }
                        
                        Toast.makeText(MainActivity.this, "账户类型已更新", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "更新账户类型失败", e);
                        Toast.makeText(MainActivity.this, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                
                listLayout.addView(accountItemView);
                
                // 添加分隔线（除了最后一个）
                if (i < accountsArray.length() - 1) {
                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(getResources().getColor(R.color.outline_variant));
                    listLayout.addView(divider);
                }
            }
            
            // 创建ScrollView包装列表
            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(listLayout);
            
            builder.setView(scrollView);
            builder.setPositiveButton("确定", null);
            builder.show();
                    
        } catch (Exception e) {
            Log.e(TAG, "读取账号列表失败", e);
            Toast.makeText(this, "读取账号列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCurrentAccountInfo() {
        String username = usernameInput.getText().toString().trim();
        String uuid = extractUUIDFromDisplay();
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            Toast.makeText(this, "当前账号：\n用户名: " + username + "\nUUID: " + uuid, Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上版本
            if (!Environment.isExternalStorageManager()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
            }
        } else {
            // Android 11 以下版本
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }, PERMISSION_REQUEST_CODE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "权限获取成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "缺少必要权限，部分功能可能无法正常使用", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void writeStringToFile(File file, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "写入文件失败", e);
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}