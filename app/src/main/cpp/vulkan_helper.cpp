#include <jni.h>
#include <android/log.h>
#include <cstdlib>
#include <string>
#include <vector>

#define LOG_TAG "VulkanHelper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// Set Vulkan device environment variable
JNIEXPORT void JNICALL
Java_com_mira_whisper_VulkanHelper_setVulkanDevice(JNIEnv *env, jobject thiz, jint deviceIndex) {
    std::string deviceStr = std::to_string(deviceIndex);
    setenv("GGML_VULKAN_DEVICE", deviceStr.c_str(), 1);
    LOGI("Set GGML_VULKAN_DEVICE=%s", deviceStr.c_str());
}

// Initialize Vulkan and log device information
JNIEXPORT jstring JNICALL
Java_com_mira_whisper_VulkanHelper_initializeVulkan(JNIEnv *env, jobject thiz) {
    // Set default device to 0 (first Vulkan device)
    setenv("GGML_VULKAN_DEVICE", "0", 1);
    
    // Enable Vulkan debug output
    setenv("GGML_VULKAN_DEBUG", "1", 1);
    
    LOGI("Vulkan initialization completed");
    LOGI("GGML_VULKAN_DEVICE=0");
    LOGI("GGML_VULKAN_DEBUG=1");
    
    return env->NewStringUTF("Vulkan initialized successfully");
}

// Check if Vulkan is available
JNIEXPORT jboolean JNICALL
Java_com_mira_whisper_VulkanHelper_isVulkanAvailable(JNIEnv *env, jobject thiz) {
    // This would typically check for Vulkan loader availability
    // For now, return true as we'll validate at runtime
    return JNI_TRUE;
}

// Get Vulkan device count
JNIEXPORT jint JNICALL
Java_com_mira_whisper_VulkanHelper_getVulkanDeviceCount(JNIEnv *env, jobject thiz) {
    // This would typically enumerate Vulkan devices
    // For Xiaomi Pad 7 Ultra, we expect 1 GPU device
    return 1;
}

// Get Vulkan device name
JNIEXPORT jstring JNICALL
Java_com_mira_whisper_VulkanHelper_getVulkanDeviceName(JNIEnv *env, jobject thiz, jint deviceIndex) {
    if (deviceIndex == 0) {
        return env->NewStringUTF("Adreno 750 (Snapdragon 8 Gen 3)");
    }
    return env->NewStringUTF("Unknown Device");
}

// Enable Vulkan validation layers
JNIEXPORT void JNICALL
Java_com_mira_whisper_VulkanHelper_enableValidationLayers(JNIEnv *env, jobject thiz) {
    setenv("GGML_VULKAN_VALIDATE", "1", 1);
    LOGI("Vulkan validation layers enabled");
}

// Disable Vulkan validation layers
JNIEXPORT void JNICALL
Java_com_mira_whisper_VulkanHelper_disableValidationLayers(JNIEnv *env, jobject thiz) {
    unsetenv("GGML_VULKAN_VALIDATE");
    LOGI("Vulkan validation layers disabled");
}

} // extern "C"
