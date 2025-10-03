# FAISS Native Library Setup

This directory should contain the prebuilt FAISS libraries for Android.

## Required Files

### Headers (in `include/`)
- `faiss/IndexFlat.h`
- `faiss/IndexIVFPQ.h` 
- `faiss/IndexHNSW.h`
- `faiss/index_io.h`
- Other FAISS headers as needed

### Libraries (in `lib/arm64-v8a/`)
- `libfaiss.a` - Main FAISS library
- `libopenblas.a` - BLAS library (if FAISS was built with BLAS support)

## Building FAISS for Android

To build FAISS for Android ARM64:

```bash
# Clone FAISS
git clone https://github.com/facebookresearch/faiss.git
cd faiss

# Build with CMake for Android
mkdir build-android
cd build-android

cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-21 \
  -DCMAKE_BUILD_TYPE=Release \
  -DFAISS_ENABLE_GPU=OFF \
  -DFAISS_ENABLE_PYTHON=OFF \
  -DBUILD_SHARED_LIBS=OFF

make -j$(nproc)
```

## Alternative: Use Prebuilt Libraries

You can also use prebuilt FAISS libraries from:
- Maven Central (if available)
- Conda-forge Android packages
- Custom builds from FAISS community

## No-BLAS Option

For smaller builds, FAISS can be compiled without BLAS support:
- Remove BLAS dependencies from CMakeLists.txt
- Use FAISS's built-in LAPACK implementation
- Results in larger library but fewer dependencies

## Integration Notes

- The JNI bridge expects FAISS to be statically linked
- Ensure compatibility with Android NDK version
- Test on target devices for performance characteristics
- Consider memory usage for large indexes
