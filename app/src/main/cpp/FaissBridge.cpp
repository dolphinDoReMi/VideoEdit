#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "FaissBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Stub implementation for testing without FAISS native libraries
// This allows the Kotlin code to compile and run tests

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_createFlatIP(JNIEnv*, jclass, jint dim) {
    LOGI("Stub: createFlatIP(dim=%d)", dim);
    return 1L; // Return non-zero handle for testing
}

JNIEXPORT jlong JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_createIVFPQ(JNIEnv*, jclass, jint dim, jint nlist, jint m, jint nbits) {
    LOGI("Stub: createIVFPQ(dim=%d, nlist=%d, m=%d, nbits=%d)", dim, nlist, m, nbits);
    return 2L; // Return non-zero handle for testing
}

JNIEXPORT jlong JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_createHNSWIP(JNIEnv*, jclass, jint dim, jint M) {
    LOGI("Stub: createHNSWIP(dim=%d, M=%d)", dim, M);
    return 3L; // Return non-zero handle for testing
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_setNProbe(JNIEnv*, jclass, jlong handle, jint nprobe) {
    LOGI("Stub: setNProbe(handle=%lld, nprobe=%d)", (long long)handle, nprobe);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_setEfSearch(JNIEnv*, jclass, jlong handle, jint efSearch) {
    LOGI("Stub: setEfSearch(handle=%lld, efSearch=%d)", (long long)handle, efSearch);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_setEfConstruction(JNIEnv*, jclass, jlong handle, jint efConstruction) {
    LOGI("Stub: setEfConstruction(handle=%lld, efConstruction=%d)", (long long)handle, efConstruction);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_train(JNIEnv* env, jclass, jlong handle, jfloatArray trainVecs) {
    jsize len = env->GetArrayLength(trainVecs);
    LOGI("Stub: train(handle=%lld, len=%d)", (long long)handle, (int)len);
    // Stub implementation - no actual training
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_addWithIds(JNIEnv* env, jclass, jlong handle, jfloatArray vecs, jlongArray ids) {
    jsize nvecs = env->GetArrayLength(vecs);
    jsize nids = env->GetArrayLength(ids);
    LOGI("Stub: addWithIds(handle=%lld, nvecs=%d, nids=%d)", (long long)handle, (int)nvecs, (int)nids);
    // Stub implementation - no actual addition
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_writeIndex(JNIEnv* env, jclass, jlong handle, jstring path) {
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    LOGI("Stub: writeIndex(handle=%lld, path=%s)", (long long)handle, pathStr);
    env->ReleaseStringUTFChars(path, pathStr);
    // Stub implementation - no actual writing
}

JNIEXPORT jlong JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_readIndex(JNIEnv* env, jclass, jstring path) {
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    LOGI("Stub: readIndex(path=%s)", pathStr);
    env->ReleaseStringUTFChars(path, pathStr);
    return 4L; // Return non-zero handle for testing
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_search(JNIEnv* env, jclass, jlong handle, jfloatArray queries, jint k, jfloatArray distances, jlongArray labels) {
    jsize nq = env->GetArrayLength(queries);
    LOGI("Stub: search(handle=%lld, nq=%d, k=%d)", (long long)handle, (int)nq, k);
    
    // Fill with dummy results for testing
    jfloat* pd = env->GetFloatArrayElements(distances, nullptr);
    jlong* pl = env->GetLongArrayElements(labels, nullptr);
    
    for (int i = 0; i < k; i++) {
        pd[i] = 1.0f - (i * 0.1f); // Decreasing similarity scores
        pl[i] = i; // Sequential IDs
    }
    
    env->ReleaseFloatArrayElements(distances, pd, 0);
    env->ReleaseLongArrayElements(labels, pl, 0);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_freeIndex(JNIEnv*, jclass, jlong handle) {
    LOGI("Stub: freeIndex(handle=%lld)", (long long)handle);
    // Stub implementation - no actual cleanup needed
}

}