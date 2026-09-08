package cn.qcofa.com.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.qcofa.com.data.Account
import cn.qcofa.com.data.AccountRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val accountRepo = remember { AccountRepository(context) }

    // 账户配置状态
    var username by remember { mutableStateOf("") }
    var customUuid by remember { mutableStateOf("") }
    var uuidDisplay by remember { mutableStateOf("UUID: ") }
    var userType by remember { mutableStateOf("msa") }
    var expandedSection by remember { mutableStateOf(false) }

    // 用户配置状态
    var acceptedLegal by remember { mutableStateOf(true) }
    var devModsEnabled by remember { mutableStateOf(false) }
    var customRamEnabled by remember { mutableStateOf(false) }
    var ramValue by remember { mutableStateOf("2048") }
    var demoMode by remember { mutableStateOf(false) }

    // 账号列表
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var showAccountsDialog by remember { mutableStateOf(false) }

    // 版本管理
    var showSkinDialog by remember { mutableStateOf(false) }

    // 加载已有配置
    LaunchedEffect(Unit) {
        val config = accountRepo.loadLauncherConfig()
        if (config != null) {
            acceptedLegal = config.acceptedLegal
            devModsEnabled = config.setDevMods
            customRamEnabled = config.setCustomRAM
            ramValue = config.customRAMValue
            accounts = config.accounts
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === 卡片1：离线账户配置 ===
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "配置离线账户文件",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; autoGenerateUuid(accountRepo, it, customUuid, onUuid = { uuidDisplay = it }) },
                        label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // 可折叠区域
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSection = !expandedSection }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (expandedSection) "▼" else "▶",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "其他功能",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    AnimatedVisibility(visible = expandedSection) {
                        Column {
                            OutlinedTextField(
                                value = customUuid,
                                onValueChange = {
                                    customUuid = it
                                    if (it.isEmpty()) autoGenerateUuid(accountRepo, username, "", onUuid = { uuidDisplay = it })
                                    else uuidDisplay = "UUID: $it"
                                },
                                label = { Text("自定义UUID (留空则自动生成)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "用户类型",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = userType,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("msa", "offline").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = { userType = type; expanded = false }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = uuidDisplay,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (username.isEmpty()) {
                                Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val uuid = extractUuid(uuidDisplay)
                            if (accountRepo.createAccountFile(username, uuid, userType, demoMode)) {
                                accountRepo.updateLauncherConf(username, uuid, acceptedLegal, devModsEnabled, customRamEnabled, ramValue, demoMode)
                                Toast.makeText(context, "账号文件创建成功", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "创建失败", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("创建账号文件") }

                    Spacer(Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            accounts = accountRepo.loadAccounts()
                            showAccountsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("查看已创建账号列表") }
                }
            }
        }

        // === 卡片2：用户简单配置 ===
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "用户简单配置",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已接受法律条款 (acceptedLegal)", color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = acceptedLegal, onCheckedChange = { acceptedLegal = it })
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用开发者模组 (setDevMods)", color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = devModsEnabled, onCheckedChange = { devModsEnabled = it })
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自定义内存 (setCustomRAM)", color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = customRamEnabled, onCheckedChange = { customRamEnabled = it })
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ramValue,
                        onValueChange = { ramValue = it },
                        label = { Text("自定义内存值 (如: 2048)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("演示模式 (isDemoMode)", color = MaterialTheme.colorScheme.onSurface)
                        Switch(checked = demoMode, onCheckedChange = { demoMode = it })
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (username.isEmpty()) {
                                Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val uuid = extractUuid(uuidDisplay)
                            accountRepo.updateLauncherConf(username, uuid, acceptedLegal, devModsEnabled, customRamEnabled, ramValue, demoMode)
                            Toast.makeText(context, "配置文件保存成功", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("保存配置文件") }

                    Spacer(Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuestCraftPlusPlus/android-openjdk-build-multiarch/releases/tag/jre22-6.0.0"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("手动安装JRE Runtime") }
                }
            }
        }

        // === 卡片3：版本管理 ===
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "版本管理",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showSkinDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("皮肤更换") }

                    Spacer(Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            if (accountRepo.saveVersionListToStorage()) {
                                Toast.makeText(context, "版本列表已保存", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) { Text("保存版本列表") }

                    Text(
                        text = "此选项在您没有魔法上网时下载不到版本列表时才用得到",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // 账号列表对话框
    if (showAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showAccountsDialog = false },
            title = { Text("已创建的账号列表") },
            text = {
                if (accounts.isEmpty()) {
                    Text("暂无已创建的账号")
                } else {
                    Column {
                        accounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("用户名: ${account.username}", fontWeight = FontWeight.Medium)
                                    Text("UUID: ${account.uuid}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                                AssistChip(
                                    onClick = { /* toggle type */ },
                                    label = {
                                        Text(
                                            if (account.accountType == "premium") "正版账户" else "离线账户",
                                            fontSize = 12.sp
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountsDialog = false }) { Text("确定") }
            }
        )
    }

    // 皮肤更换对话框
    if (showSkinDialog) {
        val versions = remember { mutableStateOf<List<String>>(emptyList()) }
        LaunchedEffect(Unit) {
            versions.value = accountRepo.loadSupportedVersions()
        }
        AlertDialog(
            onDismissRequest = { showSkinDialog = false },
            title = { Text("选择皮肤版本") },
            text = {
                LazyColumn {
                    items(versions.value) { version ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(version, color = MaterialTheme.colorScheme.onSurface)
                                Text("⬇", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSkinDialog = false }) { Text("关闭") }
            }
        )
    }
}

private fun autoGenerateUuid(repo: AccountRepository, username: String, customUuid: String, onUuid: (String) -> Unit) {
    if (username.isNotEmpty() && customUuid.isEmpty()) {
        onUuid("UUID: ${repo.generateUUID(username)}")
    }
}

private fun extractUuid(uuidDisplay: String): String {
    return if (uuidDisplay.startsWith("UUID: ")) uuidDisplay.substring(6) else uuidDisplay
}

