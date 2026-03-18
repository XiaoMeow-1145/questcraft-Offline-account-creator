# questcraft-Offline-account-creator
Used for creating offline accounts in QuestCraft, providing a creator tool for players who don’t have the official version.

** The implementation principle involves modifying the Android directory to download the trial account from the /data/user/0/com.qcxr.qcxr/files/accounts directory.
Modified content: "isDemoMode": false.
"username":
"uuid":
After making these changes, it will function as an offline account. Additionally, the file in storage/emulated/0/Android/data/com.qcxr.qcxr/files/launcher.conf needs to be modified so that the account information can be properly displayed.
{
"acceptedLegal": true,
"setDevMods": false,
"setCustomRAM": false,
"customRAMValue": "2048",
"lastSelectedInstance": 0,
"lastSelectedAccount": 0,
"accounts": [
    {
"username": "34646",
"uuid": "6ef82f9f-e9b8-0440-56a9-6fdf5666b0d3"
}
]
}
In this way, you have successfully added an account. However, please note that if you’ve previously installed the game version, you don’t need to be connected to the internet. The account will only take effect when you start the game with the internet disconnected. If you’re connected to the internet, the account won’t function.
