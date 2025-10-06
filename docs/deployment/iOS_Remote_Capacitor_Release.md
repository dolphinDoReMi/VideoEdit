# iOS Remote (Capacitor) Release — Build, Archive, Upload

When frontend or iOS native config changes, use this non-interactive workflow.

## Prereqs
- Xcode + Command Line Tools
- CocoaPods installed
- Corepack + pnpm
- Optional: App Store Connect API key env vars: `ASC_API_KEY_ID`, `ASC_API_ISSUER_ID`, `ASC_API_KEY_PATH`

## Steps
```bash
# 0) Ensure pnpm via Corepack
corepack enable && pnpm --version

# 1) Install deps and verify web build
pnpm install --frozen-lockfile
pnpm build

# 2) Sync Capacitor iOS and CocoaPods
pnpm exec cap sync ios
cd ios/App && pod install --repo-update && cd -

# 3) Bump build number, archive Release
cd ios/App
agvtool next-version -all
cd -
xcodebuild -workspace ios/App/App.xcworkspace \
  -scheme App -configuration Release \
  -destination 'generic/platform=iOS' \
  -allowProvisioningUpdates \
  clean archive \
  -archivePath ios/build/App.xcarchive

# 4) Export/Upload (conditional)
if [[ -n "$ASC_API_KEY_ID" && -n "$ASC_API_ISSUER_ID" && -n "$ASC_API_KEY_PATH" ]]; then
  mkdir -p ios/build
  cat > ios/build/ExportOptions.plist <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>app-store</string>
  <key>signingStyle</key>
  <string>automatic</string>
  <key>uploadSymbols</key>
  <true/>
</dict>
</plist>
PLIST

  xcodebuild -exportArchive \
    -archivePath ios/build/App.xcarchive \
    -exportOptionsPlist ios/build/ExportOptions.plist \
    -exportPath ios/build

  IPA=$(ls ios/build/*.ipa | head -n1)
  if [[ -n "$IPA" ]]; then
    xcrun altool --upload-app -f "$IPA" -t ios \
      --apiKey "$ASC_API_KEY_ID" --apiIssuer "$ASC_API_ISSUER_ID" \
      --verbose
  fi
else
  open ios/build/App.xcarchive
fi
```

## When to use
- Changes in `capacitor.config.ts`, `ios/**`, `**/Info.plist`, `app/**`, or `components/**`

## Notes
- Safe to re-run; idempotent sync and archive.
- For manual upload, Organizer opens when API key not provided.
