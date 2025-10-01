# Mira Release Keystore Setup

## Generate Release Keystore

To generate a release keystore for Mira, run the following command:

```bash
keytool -genkey -v -keystore mira-release.keystore -alias mira -keyalg RSA -keysize 2048 -validity 10000
```

## Keystore Configuration

Update the following in `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../keystore/mira-release.keystore")
        storePassword = "YOUR_STORE_PASSWORD"
        keyAlias = "mira"
        keyPassword = "YOUR_KEY_PASSWORD"
    }
}
```

## Security Notes

- **NEVER** commit the keystore file to version control
- Store passwords securely (use environment variables in CI/CD)
- Keep backup copies of the keystore in secure locations
- Use different keystores for debug and release builds

## Environment Variables (Recommended)

For CI/CD pipelines, use environment variables:

```kotlin
storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "fallback_password"
keyPassword = System.getenv("KEY_PASSWORD") ?: "fallback_password"
```

## Xiaomi Store Requirements

- Keystore must be valid for at least 25 years
- Use RSA 2048-bit or higher encryption
- Keystore must be signed with SHA-256 or higher

## Backup Strategy

1. Create encrypted backup of keystore
2. Store in multiple secure locations
3. Document the keystore details securely
4. Test keystore restoration process regularly