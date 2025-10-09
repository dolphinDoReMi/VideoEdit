cmake_minimum_required(VERSION 3.22)
project(whisper_jni)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Whisper source files (using stub implementation - no GGML dependencies)
set(WHISPER_SOURCES
    whisper_stub.cpp
    whisper_jni.cpp
    whisper_loader.cpp
    vulkan_helper.cpp
)

# Build whisper_jni library
add_library(whisper_jni SHARED ${WHISPER_SOURCES})

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Find libraries
find_library(log-lib log)

target_link_libraries(whisper_jni
    android
    log
)
