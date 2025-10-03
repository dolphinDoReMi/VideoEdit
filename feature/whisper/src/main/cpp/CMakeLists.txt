cmake_minimum_required(VERSION 3.22.1)
project(whisper_jni)
add_library(whisper_jni SHARED
  whisper_jni.cpp
  # add whisper.cpp sources here or link as submodule
)
find_library(log-lib log)
target_link_libraries(whisper_jni ${log-lib})
