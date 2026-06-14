package cn.qcofa.com;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public class HomeFragment extends Fragment {

    private EditText usernameInput;
    private EditText customUuidInput;
    private Spinner userTypeSpinner;
    private TextView uuidDisplay;
    private EditText ramValueInput;
    private CheckBox legalCheck, devModsCheck, customRamCheck, demoModeCheck;
    private MaterialButton createAccountBtn, saveConfigBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化 UI 组件
        initViews(view);

        // 加载并显示当前账号信息
        loadAndShowCurrentAccount();

        // 设置按钮点击事件
        setupClickListeners(view);
    }

    private void initViews(View view) {
        usernameInput = view.findViewById(R.id.usernameInput);
        customUuidInput = view.findViewById(R.id.customUuidInput);
        userTypeSpinner = view.findViewById(R.id.userTypeSpinner);
        uuidDisplay = view.findViewById(R.id.uuidDisplay);
        ramValueInput = view.findViewById(R.id.ramValueInput);

        legalCheck = view.findViewById(R.id.legalCheck);
        devModsCheck = view.findViewById(R.id.devModsCheck);
        customRamCheck = view.findViewById(R.id.customRamCheck);
        demoModeCheck = view.findViewById(R.id.demoModeCheck);

        createAccountBtn = view.findViewById(R.id.createAccountBtn);
        saveConfigBtn = view.findViewById(R.id.saveConfigBtn);

        // 设置用户类型选择器
        setupUserTypeSpinner();

        // 设置默认值
        ramValueInput.setText("2048");
    }

    private void setupUserTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"msa", "offline"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(adapter);

        // 设置默认选中项为 "msa"
        userTypeSpinner.setSelection(0);
    }

    private void setupClickListeners(View view) {
        // 创建账号按钮
        createAccountBtn.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();
            createAccountFile();
        });

        // 保存配置按钮
        saveConfigBtn.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            ).start();
            saveConfigFiles();
        });

        // 为用户名输入框添加文本变化监听器，自动触发 UUID 生成
        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                autoGenerateUUIDIfNeeded();
            }
        });

        // 为自定义 UUID 输入框添加文本变化监听器
        customUuidInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String customUuid = customUuidInput.getText().toString().trim();
                if (!customUuid.isEmpty()) {
                    uuidDisplay.setText(customUuid);
                } else {
                    autoGenerateUUIDIfNeeded();
                }
            }
        });
    }

    private void autoGenerateUUIDIfNeeded() {
        String customUuid = customUuidInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();

        // 如果自定义 UUID 输入框为空且用户名不为空，则自动生成 UUID
        if (customUuid.isEmpty() && !username.isEmpty()) {
            String offline = "offline player:" + username;
            String md5 = md5Hash(offline);

            // 格式化为 UUID 格式
            if (md5.length() >= 32) {
                String formattedUUID = String.format("%s-%s-%s-%s-%s",
                        md5.substring(0, 8),
                        md5.substring(8, 12),
                        md5.substring(12, 16),
                        md5.substring(16, 20),
                        md5.substring(20, 32)
                );
                uuidDisplay.setText(formattedUUID);
            } else {
                Toast.makeText(requireContext(), "UUID 生成失败", Toast.LENGTH_SHORT).show();
            }
        } else if (!customUuid.isEmpty()) {
            uuidDisplay.setText(customUuid);
        }
    }

    private String getUserType() {
        return userTypeSpinner.getSelectedItem().toString();
    }

    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    private void createAccountFile() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(requireContext(), "请先生成 UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 创建存储目录
            File storageDir = new File(requireContext().getExternalFilesDir(null), "questcraft_accounts");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // 创建账号 JSON 文件
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
            writer.write(accountJson.toString(2));
            writer.close();

            Toast.makeText(requireContext(), "账号文件创建成功: " + jsonFile.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "创建账号文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateLauncherConf(String username, String uuid) {
        try {
            // 创建 launcher.conf 文件
            File storageDir = new File(requireContext().getExternalFilesDir(null), "questcraft_accounts");
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
                        // 避免重复添加相同 UUID 的账号
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
            writer.write(confJson.toString(2));
            writer.close();

            Toast.makeText(requireContext(), "配置文件更新成功: " + confFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "更新配置文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveConfigFiles() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        String uuid = extractUUIDFromDisplay();
        if (uuid == null || uuid.isEmpty()) {
            Toast.makeText(requireContext(), "请先生成 UUID", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLauncherConf(username, uuid);

        // 保存当前账号信息到 SharedPreferences
        saveCurrentAccount(username, uuid);

        // 显示当前账号信息
        showCurrentAccountInfo();
    }

    private void saveCurrentAccount(String username, String uuid) {
        SharedPreferences prefs = requireContext().getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("uuid", uuid);
        editor.apply();
    }

    private void loadAndShowCurrentAccount() {
        SharedPreferences prefs = requireContext().getSharedPreferences("current_account", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String uuid = prefs.getString("uuid", "");

        if (!username.isEmpty() && !uuid.isEmpty()) {
            usernameInput.setText(username);
            uuidDisplay.setText(uuid);
        }
    }

    private void showCurrentAccountInfo() {
        String username = usernameInput.getText().toString().trim();
        String uuid = extractUUIDFromDisplay();

        if (!username.isEmpty() && !uuid.isEmpty()) {
            Toast.makeText(requireContext(), "当前账号：\n用户名: " + username + "\nUUID: " + uuid, Toast.LENGTH_LONG).show();
        }
    }

    private String extractUUIDFromDisplay() {
        String uuidText = uuidDisplay.getText().toString().trim();
        if (uuidText.isEmpty() || uuidText.equals("-")) {
            return null;
        }
        return uuidText;
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
}
