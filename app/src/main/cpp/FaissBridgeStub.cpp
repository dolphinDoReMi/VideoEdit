#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "FaissBridgeStub"
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
Java_com_mira_clip_index_faiss_FaissBridge_setNProbe(JNIEnv*, jclass, jlong h, jint nprobe) {
    LOGI("Stub: setNProbe(handle=%lld, nprobe=%d)", h, nprobe);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_setEfSearch(JNIEnv*, jclass, jlong h, jint ef) {
    LOGI("Stub: setEfSearch(handle=%lld, ef=%d)", h, ef);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_setEfConstruction(JNIEnv*, jclass, jlong h, jint efc) {
    LOGI("Stub: setEfConstruction(handle=%lld, efc=%d)", h, efc);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_train(JNIEnv* env, jclass, jlong h, jfloatArray arr) {
    jsize n = env->GetArrayLength(arr);
    LOGI("Stub: train(handle=%lld, array_size=%d)", h, n);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_addWithIds(JNIEnv* env, jclass, jlong h, jfloatArray x, jlongArray ids) {
    jsize nx = env->GetArrayLength(x);
    jsize ni = env->GetArrayLength(ids);
    LOGI("Stub: addWithIds(handle=%lld, vectors=%d, ids=%d)", h, nx, ni);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_writeIndex(JNIEnv* env, jclass, jlong h, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    LOGI("Stub: writeIndex(handle=%lld, path=%s)", h, p);
    env->ReleaseStringUTFChars(path, p);
}

JNIEXPORT jlong JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_readIndex(JNIEnv* env, jclass, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    LOGI("Stub: readIndex(path=%s)", p);
    env->ReleaseStringUTFChars(path, p);
    return 4L; // Return non-zero handle for testing
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_search(JNIEnv* env, jclass, jlong h, jfloatArray q, jint k, jfloatArray dist, jlongArray lab) {
    jsize nq = env->GetArrayLength(q);
    LOGI("Stub: search(handle=%lld, query_size=%d, k=%d)", h, nq, k);
    
    // Fill with dummy results for testing
    jfloat* pd = env->GetFloatArrayElements(dist, nullptr);
    jlong* pl = env->GetLongArrayElements(lab, nullptr);
    
    for (int i = 0; i < k; i++) {
        pd[i] = 1.0f - (i * 0.1f); // Decreasing similarity scores
        pl[i] = i + 1; // Sequential IDs
    }
    
    env->ReleaseFloatArrayElements(dist, pd, 0);
    env->ReleaseLongArrayElements(lab, pl, 0);
}

JNIEXPORT void JNICALL
Java_com_mira_clip_index_faiss_FaissBridge_freeIndex(JNIEnv*, jclass, jlong h) {
    LOGI("Stub: freeIndex(handle=%lld)", h);
}

} // extern "C"
