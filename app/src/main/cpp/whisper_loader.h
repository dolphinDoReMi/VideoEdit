#pragma once
#include "whisper.h"

// Single loader function with comprehensive logging
whisper_context* load_model_or_throw(const char* abs_path);

// FD-based loader for scoped storage compliance
whisper_context* load_model_from_fd_or_throw(int fd);

// Forbid direct calls to prevent multiple entry points
#define whisper_init_from_file_with_params(...)  static_assert(false, "Use load_model_or_throw()")
#define whisper_init_from_file(...)              static_assert(false, "Use load_model_or_throw()")
