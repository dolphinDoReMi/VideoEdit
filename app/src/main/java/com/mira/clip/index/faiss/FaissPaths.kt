package com.mira.clip.index.faiss

import android.content.Context
import java.io.File

object FaissPaths {
    fun variantRoot(ctx: Context, variant: String) =
        File(FaissConfig.resolveRoot(ctx), variant).apply { mkdirs() }
    
    fun staging(ctx: Context, variant: String) =
        File(variantRoot(ctx, variant), ".staging").apply { mkdirs() }
    
    fun segments(ctx: Context, variant: String) =
        File(variantRoot(ctx, variant), "segments").apply { mkdirs() }
    
    fun shards(ctx: Context, variant: String) =
        File(variantRoot(ctx, variant), "shards").apply { mkdirs() }
    
    fun manifest(ctx: Context, variant: String) =
        File(variantRoot(ctx, variant), "MANIFEST.json")
    
    fun segFiles(ctx: Context, variant: String, ts: Long, count: Int): Pair<File, File> {
        val dir = segments(ctx, variant)
        return File(dir, "seg-$ts-$count.faiss") to File(dir, "seg-$ts-$count.ids.json")
    }
    
    fun segFilesTmp(ctx: Context, variant: String, ts: Long, count: Int): Pair<File, File> {
        val dir = staging(ctx, variant)
        return File(dir, "seg-$ts-$count.faiss.tmp") to File(dir, "seg-$ts-$count.ids.json.tmp")
    }
}
