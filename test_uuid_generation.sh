#!/bin/sh
# 测试UUID生成算法是否与Android应用一致

echo "测试UUID生成算法..."

# 输入用户名
NAME="MC_Player"
echo "用户名: $NAME"

# 生成离线UUID (shell脚本算法)
OFFLINE="offline player:$NAME"
MD5=$(echo -n "$OFFLINE" | md5sum | awk '{print substr($1,1,32)}')
UUID=$(echo "$MD5" | sed 's/^\(.\{8\}\)\(.\{4\}\)\(.\{4\}\)\(.\{4\}\)\(.\{12\}\)$/\1-\2-\3-\4-\5/')

echo "Shell脚本生成的UUID: $UUID"

echo ""
echo "请在Android应用中输入用户名 'MC_Player' 并生成UUID，"
echo "然后比较结果是否与上面的UUID一致。"