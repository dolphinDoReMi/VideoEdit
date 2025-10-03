package com.mira.clip.retrieval.util
import java.io.File

object FileIO {
  fun ensureDir(path: String) { val d = File(path); if (!d.exists()) d.mkdirs() }
}
