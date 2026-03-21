# questcraft-Offline-account-creator
 Used for creating offline accounts in QuestCraft, Provide offline accounts for pirated players
 
## Suitable For Quest, Pico, YVR
 
## How to achieve it
The implementation principle involves modifying the Android directory the DemoMode account from the  [/data/user/0/com.qcxr.qcxr/files/accounts] Get this file (you Android root access required) The username can be any name, but the UUID may require a specific value Change the DemoMode to false 
Modified content:
```bash
"isDemoMode": false,
"username": ""
"uuid": ""
```
After making these changes, it will function as an offline account, Additionally, the file in  [storage/emulated/0/Android/data/com.qcxr.qcxr/files/launcher.conf]  needs to be modified so that the account information can be properly displayed:
```bash
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
```
In this way you have successfully added an account However please note that if you’ve previously installed the game version, you don’t need to be connected to the internet The account will only take effect when you start the game with the internet disconnected If you’re reconnected to the internet the account won’t function

# How to use it
Download the first alpha version for testing purposes. Note that it’s .sh (So you can only use it on a terminal or any software that comes with its own terminal) executable file, and it’s currently intended for use by Chinese users
