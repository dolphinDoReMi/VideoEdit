cmake_minimum_required(VERSION 3.22)
project(faiss_jni)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Whisper source files (now consolidated in app module)
set(WHISPER_SOURCES
    whisper.cpp
    whisper_jni.cpp
    whisper_loader.cpp
    vulkan_helper.cpp
)

# Check if FAISS libraries exist
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/lib/${ANDROID_ABI}/libfaiss.a")
    # Real FAISS implementation with Whisper
    add_library(faiss_jni SHARED FaissBridge.cpp ${WHISPER_SOURCES})
    
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/include)
    include_directories(${CMAKE_CURRENT_SOURCE_DIR})
    
    add_library(faiss STATIC IMPORTED)
    set_target_properties(faiss PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/lib/${ANDROID_ABI}/libfaiss.a)

    # Find libraries
    find_library(log-lib log)

    target_link_libraries(faiss_jni
        faiss
        android
        log
    )
    
else()
    # Stub implementation for testing without FAISS, but with Whisper
    add_library(faiss_jni SHARED FaissBridgeStub.cpp ${WHISPER_SOURCES})
    
    include_directories(${CMAKE_CURRENT_SOURCE_DIR})
    
    # Find libraries
    find_library(log-lib log)
    
    target_link_libraries(faiss_jni
        android
        log
    )
endif()
