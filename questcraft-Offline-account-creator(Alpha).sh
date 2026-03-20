#!/bin/sh
clear

echo "===== QuestCraft 离线账号创建器 ====="
echo "      适用于 YVR / Quest / Pico"
echo "=========================================="

PKG="com.qcxr.qcxr"

# 输入用户名
echo -n "输入玩家名称："
read NAME
if [ -z "$NAME" ]; then
    NAME="MC_Player"
fi

# 生成离线UUID
OFFLINE="offline player:$NAME"
MD5=$(echo -n "$OFFLINE" | md5sum | awk '{print substr($1,1,32)}')
UUID=$(echo "$MD5" | sed 's/^\(.\{8\}\)\(.\{4\}\)\(.\{4\}\)\(.\{4\}\)\(.\{12\}\)$/\1-\2-\3-\4-\5/')

echo "✅ 用户名：$NAME"
echo "✅ UUID：$UUID"

# 免ROOT安全路径（MT可直接读写）
STORAGE_BASE="/storage/emulated/0/Android/data/$PKG"
LAUNCHER_CONF="$STORAGE_BASE/files/launcher.conf"
ACCOUNTS_DIR="$STORAGE_BASE/files/accounts"
JSON_FILE="$ACCOUNTS_DIR/$UUID.json"

# 系统路径（只处理这里）
SYSTEM_ACCOUNTS_DIR="/data/user/0/$PKG/files/accounts"
DEMO_JSON="$SYSTEM_ACCOUNTS_DIR/00000000-0000-0000-0000-000000000000.json"

# 示例文件路径
EXAMPLE_JSON="示例_$UUID.json"
EXAMPLE_LAUNCHER="示例_launcher.conf"

# ======================
# 【新版】清理/禁用试玩账号
# 有ROOT → 删除
# 无ROOT → 重命名为 .disabled 关闭
# ======================
clean_demo_account() {
    echo ""
    echo "🧹 自动处理试玩账号..."

    if [ ! -d "$SYSTEM_ACCOUNTS_DIR" ]; then
        echo "ℹ️ 未找到账号目录，跳过"
        return
    fi

    if [ ! -f "$DEMO_JSON" ]; then
        echo "ℹ️ 未找到试玩账号，无需处理"
        return
    fi

    # 判断是不是试玩模式
    if grep -q '"isDemoMode": true' "$DEMO_JSON" 2>/dev/null; then
        # 尝试删除（有ROOT才成功）
        if rm -f "$DEMO_JSON" 2>/dev/null; then
            echo "✅ 已删除试玩账号"
        else
            # 无ROOT，重命名禁用
            mv "$DEMO_JSON" "${DEMO_JSON}.disabled" 2>/dev/null
            if [ $? -eq 0 ]; then
                echo "✅ 已禁用试玩账号（重命名为 .disabled）"
            else
                echo "⚠️  无权限处理试玩账号"
            fi
        fi
    else
        echo "ℹ️ 不是试玩账号，跳过"
    fi
}

# 查看文件的循环菜单函数
view_file_menu() {
    while true; do
        echo ""
        echo "📁 文件查看菜单"
        echo "1) 查看账号JSON文件 ($JSON_FILE)"
        echo "2) 查看 launcher.conf"
        if [ -n "$1" ] && [ "$1" = "example" ]; then
            echo "3) 查看示例账号JSON ($EXAMPLE_JSON)"
            echo "4) 查看示例 launcher.conf ($EXAMPLE_LAUNCHER)"
            echo "5) 返回主菜单"
            echo "6) 退出脚本"
        else
            echo "3) 返回主菜单"
            echo "4) 退出脚本"
        fi
        echo -n "请输入选项："
        read VIEW_CHOICE

        case "$VIEW_CHOICE" in
            1)
                if [ -f "$JSON_FILE" ]; then
                    echo -e "\n--- $JSON_FILE 内容 ---"
                    cat "$JSON_FILE"
                    echo -e "\n--- 内容结束 ---"
                else
                    echo "❌ 账号文件不存在"
                fi
                ;;
            2)
                if [ -f "$LAUNCHER_CONF" ]; then
                    echo -e "\n--- $LAUNCHER_CONF 内容 ---"
                    cat "$LAUNCHER_CONF"
                    echo -e "\n--- 内容结束 ---"
                else
                    echo "❌ launcher.conf 不存在"
                fi
                ;;
            3)
                if [ -n "$1" ] && [ "$1" = "example" ]; then
                    if [ -f "$EXAMPLE_JSON" ]; then
                        echo -e "\n--- $EXAMPLE_JSON 内容 ---"
                        cat "$EXAMPLE_JSON"
                        echo -e "\n--- 内容结束 ---"
                    else
                        echo "❌ 示例账号文件不存在"
                    fi
                else
                    echo "返回主菜单..."
                    return
                fi
                ;;
            4)
                if [ -n "$1" ] && [ "$1" = "example" ]; then
                    if [ -f "$EXAMPLE_LAUNCHER" ]; then
                        echo -e "\n--- $EXAMPLE_LAUNCHER 内容 ---"
                        cat "$EXAMPLE_LAUNCHER"
                        echo -e "\n--- 内容结束 ---"
                    else
                        echo "❌ 示例 launcher.conf 不存在"
                    fi
                else
                    echo "退出脚本"
                    exit 0
                fi
                ;;
            5)
                if [ -n "$1" ] && [ "$1" = "example" ]; then
                    echo "返回主菜单..."
                    return
                else
                    echo "输入错误，请重试"
                fi
                ;;
            6)
                if [ -n "$1" ] && [ "$1" = "example" ]; then
                    echo "退出脚本"
                    exit 0
                else
                    echo "输入错误，请重试"
                fi
                ;;
            *)
                echo "输入错误，请重试"
                ;;
        esac
    done
}

# 主操作选择
echo ""
echo "请选择操作："
echo "1) 正式创建账号"
echo "2) 生成示例（当前目录）"
echo ""
echo -n "请输入 1 或 2："
read CHOICE

if [ "$CHOICE" = "2" ]; then
    echo "🔹 生成完整示例文件..."

    cat > "$EXAMPLE_JSON" << EOF
{
  "accessToken": "0",
  "expiresOn": 0,
  "isDemoMode": false,
  "userType": "msa",
  "username": "$NAME",
  "uuid": "$UUID"
}
EOF

    cat > "$EXAMPLE_LAUNCHER" << EOF
{
  "acceptedLegal": true,
  "setDevMods": false,
  "setCustomRAM": false,
  "customRAMValue": "2048",
  "lastSelectedInstance": 0,
  "lastSelectedAccount": 0,
  "accounts": [
    {
      "username": "$NAME",
      "uuid": "$UUID"
    }
  ]
}
EOF

    echo ""
    echo "✅ 示例生成完成！"
    echo "📄 $EXAMPLE_JSON"
    echo "📄 $EXAMPLE_LAUNCHER"

    view_file_menu "example"

elif [ "$CHOICE" = "1" ]; then
    # 生成前处理试玩账号
    clean_demo_account

    echo "🔹 创建账号中..."

    if [ ! -d "$STORAGE_BASE" ]; then
        echo "❌ 未找到 $PKG 目录，无法正式生成"
        exit 1
    fi
    echo "✅ 检测到 QuestCraft 目录"

    mkdir -p "$ACCOUNTS_DIR" 2>/dev/null

    cat > "$JSON_FILE" << EOF
{
  "accessToken": "0",
  "expiresOn": 0,
  "isDemoMode": false,
  "userType": "msa",
  "username": "$NAME",
  "uuid": "$UUID"
}
EOF

    echo "✅ 账号文件已生成：$JSON_FILE"

    if [ -f "$LAUNCHER_CONF" ]; then
        echo "🔧 追加离线账号到 launcher.conf..."
        sed -i.bak "/\"accounts\": \[/a \    {\"username\":\"$NAME\",\"uuid\":\"$UUID\"}," "$LAUNCHER_CONF"
        echo "✅ 账号追加完成！格式正确"
    else
        echo "⚠️  未找到 launcher.conf，仅生成账号文件"
    fi

    echo ""
    echo "🎉 全部完成✅"
    view_file_menu

else
    echo "❌ 输入错误"
    exit 1
fi
