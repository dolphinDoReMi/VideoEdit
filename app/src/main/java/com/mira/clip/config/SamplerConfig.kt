package com.mira.clip.config

enum class Schedule { UNIFORM, TSN_JITTER, SLOWFAST_LITE }
enum class DecodeBackend { MMR, MEDIACODEC }
enum class SeekPolicy { PRECISE_OR_NEXT }

data class SamplerConfig(
    val frameCount: Int,
    val schedule: Schedule,
    val decodeBackend: DecodeBackend,
    val seekPolicy: SeekPolicy = SeekPolicy.PRECISE_OR_NEXT,
    val output: OutputSpec = OutputSpec(),
    val concurrency: Int = 1,
    val memoryBudgetMb: Int = 512
)

data class OutputSpec(
    val imageFormat: String = "PNG", // PNG or JPEG
    val jpegQuality: Int = 90,
    val writeSidecarJson: Boolean = true,
    val outDirName: String = "samples"
)
