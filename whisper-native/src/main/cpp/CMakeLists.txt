cmake_minimum_required(VERSION 3.22)
project(whisper_native)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Whisper source files with real implementation
set(WHISPER_SOURCES
    whisper_real.cpp
    whisper_jni.cpp
    whisper_loader.cpp
    vulkan_helper.cpp
)

# Build whisper_native library
add_library(whisper_native SHARED ${WHISPER_SOURCES})

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Find libraries
find_library(log-lib log)

target_link_libraries(whisper_native
    android
    log
)
