cmake_minimum_required(VERSION 3.22.1)

project("whisper_jni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add whisper.cpp (assuming it's in a subdirectory)
# For now, we'll create a minimal implementation
add_library(whisper_jni SHARED
    native-lib.cpp
)

# Find and link required libraries
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(whisper_jni
    ${log-lib}
    ${android-lib}
)
