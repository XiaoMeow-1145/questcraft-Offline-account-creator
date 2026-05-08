package cn.qcofa.com;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // 设置用户类型选择器
        setupUserTypeSpinner();

        // 设置默认值
        ramValueInput.setText("2048");
        // 不再自动设置用户名，让用户自行输入
    }

    private void setupUserTypeSpinner() {
        // 创建适配器并添加选项
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                this, 
                R.array.user_types_array, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);
        
        // 设置默认选中项为"msa"
        userTypeSpinner.setSelection(0);
    }

    private void requestPermissions() {
        // 对于我们的应用，使用应用专属存储空间，不需要外部存储权限
        // Android 11+ 不再需要 MANAGE_EXTERNAL_STORAGE 权限来访问应用专属目录
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 10及以下版本的读写权限
            String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            
            boolean needsPermission = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }
            
            if (needsPermission) {
                requestPermissions(permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void setupClickListeners() {
        Button createAccountBtn = findViewById(R.id.createAccountBtn);
        createAccountBtn.setOnClickListener(v -> createAccountFile());

        Button saveConfigBtn = findViewById(R.id.saveConfigBtn);
        saveConfigBtn.setOnClickListener(v -> saveConfigFiles());

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

    private void generateUUID() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            // 如果用户名为空，生成随机用户名
            username = "Player_" + System.currentTimeMillis() % 10000;
            usernameInput.setText(username);
        }

        // 生成离线UUID - 使用与shell脚本相同的算法
        String offline = "offline player:" + username;
        String md5 = md5Hash(offline);
        
        // 格式化为UUID格式
        if (md5.length() >= 32) {
            String formattedUUID = String.format("%s-%s-%s-%s-%s",
                md5.substring(0, 8),
                md5.substring(8, 12),
                md5.substring(12, 16),
                md5.substring(16, 20),
                md5.substring(20, 32));
            
            uuidDisplay.setText("UUID: " + formattedUUID);
            Toast.makeText(this, "UUID生成成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "UUID生成失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void autoGenerateUUIDIfNeeded() {
        // 检查自定义UUID输入框是否为空
        String customUuid = customUuidInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        
        // 如果自定义UUID输入框为空且用户名不为空，则自动生成UUID
        if (customUuid.isEmpty() && !username.isEmpty()) {
            // 生成离线UUID - 使用与shell脚本相同的算法
            String offline = "offline player:" + username;
            String md5 = md5Hash(offline);
            
            // 格式化为UUID格式
            if (md5.length() >= 32) {
                String formattedUUID = String.format("%s-%s-%s-%s-%s",
                    md5.substring(0, 8),
                    md5.substring(8, 12),
                    md5.substring(12, 16),
                    md5.substring(16, 20),
                    md5.substring(20, 32));
                
                uuidDisplay.setText("UUID: " + formattedUUID);
            } else {
                Toast.makeText(this, "UUID生成失败", Toast.LENGTH_SHORT).show();
            }
        } else if (!customUuid.isEmpty()) {
            // 如果自定义UUID不为空，则显示自定义UUID
            uuidDisplay.setText("UUID: " + customUuid);
        }
    }
    
    private String getUserType() {
        // 获取Spinner中选中的用户类型
        return userTypeSpinner.getSelectedItem().toString();
    }

    private String md5Hash(String input) {
        try {
            Log.d("QcofA", "计算MD5: " + input);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            Log.e("QcofA", "MD5哈希计算失败", e);
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
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
        android.content.SharedPreferences prefs = getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("uuid", uuid);
        editor.apply();
    }
    
    private void loadAndShowCurrentAccount() {
        android.content.SharedPreferences prefs = getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String uuid = prefs.getString("uuid", "");
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            usernameInput.setText(username);
            uuidDisplay.setText("UUID: " + uuid);
        }
    }
    
    private void showCurrentAccountInfo() {
        String username = usernameInput.getText().toString().trim();
        String uuid = extractUUIDFromDisplay();
        
        if (!username.isEmpty() && !uuid.isEmpty()) {
            Toast.makeText(this, "当前账号：\n用户名: " + username + "\nUUID: " + uuid, Toast.LENGTH_LONG).show();
        }
    }

    private String extractUUIDFromDisplay() {
        String uuidText = uuidDisplay.getText().toString();
        if (uuidText.startsWith("UUID: ")) {
            return uuidText.substring(6); // 去掉 "UUID: " 前缀
        }
        return null;
    }

    private String readFileToString(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file)));
        String line;
        
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        
        reader.close();
        return sb.toString().trim();
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
}