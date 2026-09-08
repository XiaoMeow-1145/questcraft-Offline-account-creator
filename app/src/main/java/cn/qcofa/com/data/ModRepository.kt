package cn.qcofa.com.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ModInfo(
    val projectId: String,
    val title: String,
    val description: String,
    val iconUrl: String? = null,
    val downloads: Long = 0,
    val latestVersion: String = ""
)

data class ModVersionFile(
    val url: String,
    val filename: String,
    val size: Long = 0
)

class ModRepository {

    companion object {
        private const val API_BASE = "https://api.modrinth.com/v2"
    }

    data class ModResult(
        val mods: List<ModInfo>,
        val totalHits: Int = 0
    )

    fun searchMods(query: String, page: Int = 0): ModResult {
        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val facets = java.net.URLEncoder.encode("""[["categories:fabric"]]""", "UTF-8")
            val url = URL("$API_BASE/search?query=$encodedQuery&facets=$facets&index=downloads&limit=20&offset=${page * 20}")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "QcofA/2.0 (Android)")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(text)
            val hits = json.getJSONArray("hits")
            val mods = (0 until hits.length()).map { i ->
                val hit = hits.getJSONObject(i)
                ModInfo(
                    projectId = hit.getString("project_id"),
                    title = hit.getString("title"),
                    description = hit.optString("description", ""),
                    iconUrl = hit.optString("icon_url", null),
                    downloads = hit.optLong("downloads", 0)
                )
            }
            ModResult(mods, json.optInt("total_hits", 0))
        } catch (e: Exception) {
            ModResult(emptyList())
        }
    }

    fun getLatestVersionFile(projectId: String): ModVersionFile? {
        return try {
            val loaders = java.net.URLEncoder.encode("""["fabric"]""", "UTF-8")
            val gameVersions = java.net.URLEncoder.encode("""["1.20.4","1.20.1","1.21","1.20.6","1.21.1"]""", "UTF-8")
            val url = URL("$API_BASE/project/$projectId/version?loaders=$loaders&game_versions=$gameVersions")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "QcofA/2.0 (Android)")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val arr = JSONArray(text)
            if (arr.length() == 0) return null
            val first = arr.getJSONObject(0)
            val files = first.getJSONArray("files")
            if (files.length() == 0) return null
            val file = files.getJSONObject(0)
            ModVersionFile(
                url = file.getString("url"),
                filename = file.optString("filename", "mod.jar"),
                size = file.optLong("size", 0)
            )
        } catch (_: Exception) { null }
    }

    fun getDownloadUrl(projectId: String): String? {
        return getLatestVersionFile(projectId)?.url
    }
}