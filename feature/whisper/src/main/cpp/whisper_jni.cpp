#include <jni.h>
#include <string>
#include <vector>

// TODO: integrate whisper.cpp, load GGUF model once (singleton), run decode.
// For now, return a deterministic stub with two segments.

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mira_com_feature_whisper_engine_WhisperBridge_decodeJson(
    JNIEnv* env, jobject thiz,
    jshortArray pcm16, jint sampleRate, jstring modelPath,
    jint threads, jint beam, jstring jlang, jboolean translate) {

  const char* json = "{\"segments\":["
                     "{\"t0\":0.00,\"t1\":1.10,\"text\":\"hello \"},"
                     "{\"t0\":1.10,\"t1\":2.40,\"text\":\"world\"}"
                     "]}";
  return env->NewStringUTF(json);
}
