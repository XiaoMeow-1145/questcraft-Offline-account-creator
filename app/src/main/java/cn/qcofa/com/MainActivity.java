package cn.qcofa.com;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG_HOME = "home";
    private static final String TAG_ABOUT = "about";

    private HomeFragment homeFragment;
    private AboutFragment aboutFragment;
    private String currentFragmentTag = TAG_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 Fragment
        homeFragment = new HomeFragment();
        aboutFragment = new AboutFragment();

        // 设置底部导航栏
        NavigationBarView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    switchFragment(homeFragment, TAG_HOME);
                    return true;
                } else if (itemId == R.id.nav_about) {
                    switchFragment(aboutFragment, TAG_ABOUT);
                    return true;
                }
                return false;
            });
        }

        // 默认显示 HomeFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, homeFragment, TAG_HOME)
                    .commit();
        } else {
            currentFragmentTag = savedInstanceState.getString("current_fragment", TAG_HOME);
        }

        // 请求必要的权限
        requestPermissions();
    }

    private void switchFragment(Fragment fragment, String tag) {
        if (currentFragmentTag.equals(tag)) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // 设置动画
        if (tag.equals(TAG_ABOUT)) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_in_left
            );
        } else {
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_in_right
            );
        }

        // 隐藏当前 Fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 显示目标 Fragment
        Fragment targetFragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (targetFragment == null) {
            transaction.add(R.id.fragmentContainer, fragment, tag);
        } else {
            transaction.show(targetFragment);
        }

        transaction.commit();
        currentFragmentTag = tag;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_fragment", currentFragmentTag);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
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
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                android.widget.Toast.makeText(this, "权限获取成功", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "缺少必要权限，部分功能可能无法正常使用", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
}
