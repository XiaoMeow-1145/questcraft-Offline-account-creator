# QcofA - QuestCraft 离线账号创建器

## 应用简介
QcofA 是一款专为 VR 一体机 MC 启动器 QuestCraft 设计的离线账号创建工具，适用于 Quest、Pico、YVR 等设备。

## 功能特性
1. **离线账号创建**：
   - 支持自定义用户名
   - 自动生成符合 QuestCraft 标准的 UUID
   - 创建格式化的账号 JSON 文件

2. **配置管理**：
   - 法律条款接受状态 (acceptedLegal)
   - 开发者模组设置 (setDevMods)
   - 自定义内存设置 (setCustomRAM)
   - 自定义内存值配置
   - 演示模式开关 (isDemoMode)

3. **文件管理**：
   - 自动创建 accounts 目录
   - 生成 UUID 对应的 JSON 账号文件
   - 更新 launcher.conf 配置文件

4. **终端模拟**：
   - 模拟 run-as 命令功能
   - 显示文件操作过程

## 使用方法
1. 在"用户名"字段输入您的游戏昵称
2. 点击"生成UUID"按钮创建唯一标识符
3. 配置所需的选项（法律条款、开发者模组、内存设置等）
4. 点击"创建账号文件"生成必要的配置文件
5. 使用"执行Run-As命令"功能模拟文件部署

## 注意事项
- 应用会在外部存储的私有目录中创建 questcraft_accounts 文件夹
- 生成的文件包括：{UUID}.json 和 launcher.conf
- 实际部署到 /data/user/0/com.qcxr.qcxr/files/accounts 需要 root 权限

## 技术细节
- 应用包名：cn.qcofa.com
- UUID 生成算法与原 shell 脚本保持一致
- 文件格式完全兼容 QuestCraft 启动器

## 权限说明
- 外部存储读写权限：用于创建和管理配置文件
- 网络权限：预留未来功能扩展