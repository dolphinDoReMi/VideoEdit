# UI Architecture Design and Control Knot

**Status: READY FOR VERIFICATION**

**Control knots:**
- Seedless pipeline: deterministic sampling
- Fixed preprocess: no random crop
- Same model assets: fixed hypothesis f_θ

**Implementation:**
- Deterministic sampling: uniform frame timestamps
- Fixed preprocessing: center-crop, no augmentation
- Re-ingest twice: SHA-256 hash comparison

**Verification:** Hash comparison script in `ops/verify_all.sh`

## UI-Specific Control Knots

**WebView Integration Control Knots:**
- WebView version: Chrome WebView 120+ compatibility
- JavaScript bridge: @JavascriptInterface for native calls
- State management: Centralized with broadcast updates
- Update frequency: Event-driven (responsive)
- Resource monitoring: Background service integration

**Accessibility Control Knots:**
- Screen reader support: Full WCAG compliance
- Keyboard navigation: Complete keyboard accessibility
- High contrast: System theme integration
- Font scaling: Dynamic text size support
- Focus management: Logical tab order

**Performance Control Knots:**
- UI responsiveness: 60fps target with fallback
- Memory management: Efficient WebView cleanup
- Animation level: Basic (performance balanced)
- Theme system: Auto (system integration)
- Resource efficiency: Minimal background processing

**Cross-Platform Control Knots:**
- Android: WebView with native bridge
- iOS: WKWebView with Core ML integration
- macOS Web: Progressive Web App features
- Responsive design: Multi-device layout support

**Verification Methods:**
- UI automation: `test_ui_automation.sh`
- Accessibility audit: `test_accessibility.sh`
- WebView bridge: `test_webview_bridge.sh`
- Cross-platform: `test_responsive_design.sh`