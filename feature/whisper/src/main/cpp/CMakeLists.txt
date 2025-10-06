cmake_minimum_required(VERSION 3.22.1)
project(whisper_jni)

# Simple implementation without external dependencies
add_library(whisper_jni SHARED whisper_jni.cpp)

# Find libraries
find_library(log-lib log)

# Link libraries
target_link_libraries(whisper_jni ${log-lib})

# Compile definitions
target_compile_definitions(whisper_jni PRIVATE WHISPER_VERSION="1.6.0")

# Compile options
if (NOT ${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    target_compile_options(whisper_jni PRIVATE -O3)
    target_compile_options(whisper_jni PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden)
endif()
