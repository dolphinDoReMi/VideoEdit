package com.mira.com.ui.web

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject

class StagingJsBridge(private val activity: ComponentActivity) {

  private val prefs: SharedPreferences by lazy {
    activity.getSharedPreferences("staging_prefs", Context.MODE_PRIVATE)
  }
  private val treesKey = "trees"

  private var pickTreeCB: ((Uri?)->Unit)? = null
  private val pickTree = activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    if (uri != null) {
      val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
      runCatching { activity.contentResolver.takePersistableUriPermission(uri, flags) }
      saveTree(uri)
    }
    pickTreeCB?.invoke(uri)
    pickTreeCB = null
  }

  @JavascriptInterface fun addTree(): String {
    val latch = java.util.concurrent.CountDownLatch(1)
    var out: Uri? = null
    activity.runOnUiThread {
      pickTreeCB = { uri -> out = uri; latch.countDown() }
      pickTree.launch(null)
    }
    latch.await()
    return out?.toString() ?: ""
  }

  @JavascriptInterface fun listTrees(): String {
    val arr = JSONArray()
    loadTrees().forEach { uri ->
      val u = Uri.parse(uri)
      arr.put(JSONObject().apply {
        put("uri", uri)
        put("label", guessLabel(u))
        put("authority", u.authority ?: "")
        put("isLocal", isLocalAuthority(u.authority))
      })
    }
    return arr.toString()
  }

  @JavascriptInterface fun removeTree(treeUri: String): String {
    val set = loadTrees().toMutableSet()
    set.remove(treeUri)
    prefs.edit().putStringSet(treesKey, set).apply()
    return "ok"
  }

  @JavascriptInterface fun persistedPermissions(): String {
    val perms = activity.contentResolver.persistedUriPermissions
    val arr = JSONArray()
    perms.forEach { p -> arr.put(p.uri.toString()) }
    return arr.toString()
  }

  @JavascriptInterface fun enumerateAll(filtersJson: String): String {
    val f = JSONObject(filtersJson)
    val recursive = f.optBoolean("recursive", true)
    val minDurMs = f.optLong("minDurationMs", 0L)
    val exts: Set<String> = f.optJSONArray("exts")?.let { j ->
      (0 until j.length()).map { j.getString(it).lowercase() }.toSet()
    } ?: emptySet()

    val all = mutableListOf<JSONObject>()
    loadTrees().forEach { t ->
      val root = DocumentFile.fromTreeUri(activity, Uri.parse(t)) ?: return@forEach
      enumerateTree(root, recursive) { df ->
        if (!df.isFile) return@enumerateTree
        val name = df.name ?: ""
        val ext = name.substringAfterLast('.', "").lowercase()
        if (exts.isNotEmpty() && !exts.contains(ext)) return@enumerateTree

        val meta = probe(df.uri)
        if (minDurMs > 0 && (meta.durationMs ?: 0L) < minDurMs) return@enumerateTree

        all += JSONObject().apply {
          put("uri", df.uri.toString())
          put("name", name)
          put("authority", df.uri.authority ?: "")
          put("isLocal", isLocalAuthority(df.uri.authority))
          meta.sizeBytes?.let { put("sizeBytes", it) }
          meta.durationMs?.let { put("durationMs", it) }
          meta.mime?.let { put("mime", it) }
          meta.lastModified?.let { put("lastModified", it) }
          meta.codec?.let { put("codec", it) }
        }
      }
    }

    val sorted = all.sortedWith(
      compareByDescending<JSONObject> { it.optBoolean("isLocal", false) }
        .thenByDescending { it.optLong("lastModified", 0L) }
    )

    val out = JSONArray()
    sorted.forEach { out.put(it) }
    return out.toString()
  }

  /* Optional: expose a single-file probe for UI "Probe" button */
  @JavascriptInterface fun probe(uriStr: String): String {
    val u = Uri.parse(uriStr)
    val meta = probe(u)
    return JSONObject().apply {
      meta.sizeBytes?.let { put("sizeBytes", it) }
      meta.durationMs?.let { put("durationMs", it) }
      meta.mime?.let { put("mime", it) }
      meta.lastModified?.let { put("lastModified", it) }
      meta.codec?.let { put("codec", it) }
    }.toString()
  }

  /* ----- Helpers ----- */

  private fun saveTree(uri: Uri) {
    val set = loadTrees().toMutableSet()
    set.add(uri.toString())
    prefs.edit().putStringSet(treesKey, set).apply()
  }
  private fun loadTrees(): Set<String> = prefs.getStringSet(treesKey, emptySet()) ?: emptySet()

  private fun enumerateTree(root: DocumentFile, recursive: Boolean, visit: (DocumentFile)->Unit) {
    root.listFiles().forEach { f ->
      visit(f)
      if (recursive && f.isDirectory) enumerateTree(f, recursive, visit)
    }
  }

  private fun guessLabel(u: Uri): String {
    return when {
      (u.authority ?: "").contains("externalstorage") -> "Internal Storage"
      (u.authority ?: "").contains("docs.storage")     -> "Google Drive"
      else -> u.authority ?: "Folder"
    }
  }

  private fun isLocalAuthority(auth: String?): Boolean {
    if (auth == null) return false
    return auth.contains("externalstorage") ||
           auth.contains("media.documents") ||
           auth.contains("downloads.documents") ||
           auth.contains("whatsapp") // treat on-device providers as local
  }

  data class Meta(val sizeBytes: Long?, val durationMs: Long?, val mime: String?, val lastModified: Long?, val codec: String?)

  private fun probe(uri: Uri): Meta {
    val cr = activity.contentResolver
    var size: Long? = null
    var mime: String? = cr.getType(uri)
    var lastMod: Long? = null

    runCatching {
      cr.query(uri, arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
          size = c.getLongOrNull(0)
          lastMod = c.getLongOrNull(1)
        }
      }
    }

    var duration: Long? = null
    var codec: String? = null
    runCatching {
      val mmr = MediaMetadataRetriever()
      mmr.setDataSource(activity, uri)
      duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
      codec = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
      if (mime == null) mime = codec
      mmr.release()
    }

    return Meta(size, duration, mime, lastMod, codec)
  }

  private fun Cursor.getLongOrNull(idx: Int): Long? =
    if (idx >= 0 && !isNull(idx)) getLong(idx) else null
}
