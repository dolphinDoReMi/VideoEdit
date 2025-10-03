cmake_minimum_required(VERSION 3.22)
project(faiss_jni)

# Check if FAISS libraries exist
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/lib/${ANDROID_ABI}/libfaiss.a")
    # Real FAISS implementation
    add_library(faiss_jni SHARED FaissBridge.cpp)
    
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/include)
    add_library(faiss STATIC IMPORTED)
    set_target_properties(faiss PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/third_party/faiss/lib/${ANDROID_ABI}/libfaiss.a)

    target_link_libraries(faiss_jni
        faiss
        android
        log
    )
else()
    # Stub implementation for testing without FAISS
    add_library(faiss_jni SHARED FaissBridge.cpp)
    
    target_link_libraries(faiss_jni
        android
        log
    )
endif()
