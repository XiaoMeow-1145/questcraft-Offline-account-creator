package cn.qcofa.com.data

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

data class Account(val username: String, val uuid: String, val userType: String = "msa", val isDemoMode: Boolean = false, val accountType: String = "offline")
data class LauncherConfig(val acceptedLegal: Boolean, val setDevMods: Boolean, val setCustomRAM: Boolean, val customRAMValue: String, val accounts: List<Account>)

class AccountRepository(private val context: Context) {
    fun getStorageDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "QCOFA.COM")
        if (!dir.exists() && !dir.mkdirs()) {
            val fallback = File(context.getExternalFilesDir(null), "QCOFA.COM")
            fallback.mkdirs(); return fallback
        }
        return dir
    }
    fun getLauncherConfFile(): File = File(getStorageDir(), "launcher.conf")
    fun generateUUID(username: String): String = try {
        val md = MessageDigest.getInstance("MD5")
        val hex = BigInteger(1, md.digest("offline player:$username".toByteArray())).toString(16).padStart(32, '0')
        "%s-%s-%s-%s-%s".format(hex.substring(0,8), hex.substring(8,12), hex.substring(12,16), hex.substring(16,20), hex.substring(20,32))
    } catch (_: Exception) { java.util.UUID.randomUUID().toString() }

    fun createAccountFile(username: String, uuid: String, userType: String, isDemoMode: Boolean): Boolean = try {
        val dir = getStorageDir(); dir.mkdirs()
        File(dir, "$uuid.json").writeText(JSONObject().apply {
            put("accessToken","0"); put("expiresOn",0); put("isDemoMode",isDemoMode)
            put("userType",userType); put("username",username); put("uuid",uuid)
        }.toString(2))
        true
    } catch (_: Exception) { false }

    fun updateLauncherConf(username: String, uuid: String, acceptedLegal: Boolean, setDevMods: Boolean, setCustomRAM: Boolean, customRAMValue: String, isDemoMode: Boolean): Boolean = try {
        val confFile = getLauncherConfFile()
        val existing = if (confFile.exists()) {
            val j = JSONObject(confFile.readText())
            val arr = j.optJSONArray("accounts")
            if (arr != null) (0 until arr.length()).map { arr.getJSONObject(it) }.filter { it.getString("uuid") != uuid }.toMutableList() else mutableListOf()
        } else mutableListOf()
        val json = JSONObject().apply {
            put("acceptedLegal", acceptedLegal); put("setDevMods", setDevMods); put("setCustomRAM", setCustomRAM)
            put("customRAMValue", customRAMValue.ifEmpty { "2048" }); put("lastSelectedInstance",0); put("lastSelectedAccount",0); put("isDemoMode",isDemoMode)
            put("accounts", JSONArray().apply { existing.forEach { put(it) }; put(JSONObject().apply { put("username",username); put("uuid",uuid) }) })
        }
        confFile.writeText(json.toString(2)); true
    } catch (_: Exception) { false }

    fun loadLauncherConfig(): LauncherConfig? = try {
        val json = JSONObject(getLauncherConfFile().readText())
        val arr = json.optJSONArray("accounts")
        val accounts = if (arr != null) (0 until arr.length()).map { Account(it.getString("username"), it.getString("uuid"), accountType = it.optString("accountType","offline")) } else emptyList()
        LauncherConfig(json.optBoolean("acceptedLegal",true), json.optBoolean("setDevMods",false), json.optBoolean("setCustomRAM",false), json.optString("customRAMValue","2048"), accounts)
    } catch (_: Exception) { null }

    fun loadAccounts(): List<Account> = loadLauncherConfig()?.accounts ?: emptyList()

    fun toggleAccountType(uuid: String, newType: String): Boolean = try {
        val json = JSONObject(getLauncherConfFile().readText())
        val arr = json.getJSONArray("accounts")
        for (i in 0 until arr.length()) { if (arr.getJSONObject(i).getString("uuid") == uuid) { arr.getJSONObject(i).put("accountType", newType); break } }
        getLauncherConfFile().writeText(json.toString(2)); true
    } catch (_: Exception) { false }

    fun saveVersionListToStorage(): Boolean = try {
        val dest = File(getStorageDir(), "supportedVersions.json")
        context.assets.open("supportedVersions.json").use { it.copyTo(dest.outputStream()) }; true
    } catch (_: Exception) { false }

    fun loadSupportedVersions(): List<String> = try {
        val json = JSONObject(context.assets.open("supportedVersions.json").bufferedReader().use { it.readText() })
        val arr = json.getJSONArray("supportedVersions"); (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun exportJreToDirectory(): Boolean = try {
        val dir = File(getStorageDir(), "jre_runtime"); dir.mkdirs()
        context.assets.open("JRE.zip").use { it.copyTo(File(dir, "JRE.zip").outputStream()) }; true
    } catch (_: Exception) { false }
}