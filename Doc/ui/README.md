# UI Documentation

## Architecture Design and Control Knot

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

## Full Scale Implementation Details

### Problem Disaggregation
- **Inputs**: User interactions, touch events, gesture recognition
- **Outputs**: Responsive UI updates, real-time feedback, accessibility support
- **Runtime surfaces**: WebView → JavaScript Bridge → Native Android → UI Updates
- **Isolation**: Debug variant uses applicationIdSuffix ".debug" (installs side-by-side)

### Analysis with Trade-offs
- **WebView vs Native**: Cross-platform consistency vs platform optimization. We choose WebView for rapid development
- **JavaScript Bridge**: Direct calls vs message passing. We use @JavascriptInterface for performance
- **State Management**: Local state vs centralized store. We implement centralized state with broadcast system
- **Resource Monitoring**: Polling vs event-driven. We use background service with broadcast updates

### Design Pipeline
```
User Input → WebView → JavaScript Bridge → Native Handler → State Update → UI Refresh
```

### Key Control-knots (all exposed)
- `UI_FRAMEWORK` (WebView | Native | Hybrid)
- `BRIDGE_TYPE` (JavascriptInterface | MessageChannel | Custom)
- `STATE_MANAGEMENT` (Local | Centralized | Broadcast)
- `UPDATE_FREQUENCY` (60fps | 30fps | Event-driven)
- `RESOURCE_MONITORING` (Polling | Service | Event-driven)
- `ACCESSIBILITY` (Basic | Full | Custom)
- `THEME_SYSTEM` (Light | Dark | Auto | Custom)
- `ANIMATION_LEVEL` (None | Basic | Full | Custom)

### Isolation & Namespacing
- **Broadcast actions**: `${applicationId}.action.UI_UPDATE`
- **WebView namespaces**: `${BuildConfig.APPLICATION_ID}.ui`
- **State keys**: `${applicationId}.state.*`
- **Debug install**: `applicationIdSuffix ".debug"` → all names differ automatically

### Prioritization & Rationale
- **P0**: WebView integration, JavaScript bridge, basic state management
- **P1**: Real-time resource monitoring, accessibility support, theme system
- **P2**: Advanced animations, gesture recognition, custom UI components

### Workplan to Execute
1. Scaffold WebView integration + build variants
2. Implement JavaScript bridge with @JavascriptInterface
3. State management system with broadcast updates
4. Resource monitoring integration
5. E2E test via UI automation
6. Bench/verify: responsiveness, memory usage, accessibility compliance

### Scale-out Plan: Control Knots and Impact

#### Single (one per knot)
| Knot | Choice | Rationale (technical • user goals) |
|------|--------|-----------------------------------|
| UI Framework | WebView | Tech: Cross-platform consistency; rapid development. • User: Consistent experience across devices |
| Bridge Type | JavascriptInterface | Tech: Direct native calls; minimal overhead. • User: Responsive interactions |
| State Management | Centralized | Tech: Single source of truth; predictable updates. • User: Consistent UI state |
| Update Frequency | Event-driven | Tech: Efficient resource usage; responsive to changes. • User: Smooth, reactive interface |
| Resource Monitoring | Background Service | Tech: Real-time data; minimal UI impact. • User: Live system information |
| Accessibility | Full | Tech: WCAG compliance; screen reader support. • User: Inclusive design |
| Theme System | Auto | Tech: System integration; user preference respect. • User: Comfortable viewing experience |
| Animation Level | Basic | Tech: Smooth transitions; performance balance. • User: Polished, responsive feel |

#### Ablations (combos)
| Combo | Knot changes (vs Single) | Rationale (technical • user goals) |
|-------|-------------------------|-----------------------------------|
| A. High Performance | Update Frequency: 60fps; Animation Level: None | Tech: Maximum responsiveness; minimal overhead. • User: Fastest possible interactions |
| B. Rich Experience | Animation Level: Full; Theme System: Custom | Tech: Enhanced visual appeal; advanced theming. • User: Premium, polished interface |
| C. Accessibility Focus | Accessibility: Custom; Update Frequency: Event-driven | Tech: Advanced accessibility features; efficient updates. • User: Superior accessibility support |
| D. Resource Efficient | Resource Monitoring: Polling; Animation Level: Basic | Tech: Reduced background processing; minimal animations. • User: Battery-friendly operation |

### Configuration Presets

```json
// SINGLE (default)
{
  "preset": "SINGLE",
  "ui_framework": "WebView",
  "bridge_type": "JavascriptInterface",
  "state_management": "Centralized",
  "update_frequency": "Event-driven",
  "resource_monitoring": "BackgroundService",
  "accessibility": "Full",
  "theme_system": "Auto",
  "animation_level": "Basic"
}

// A: HIGH_PERFORMANCE
{ "preset": "HIGH_PERFORMANCE", "update_frequency": "60fps", "animation_level": "None" }

// B: RICH_EXPERIENCE
{ "preset": "RICH_EXPERIENCE", "animation_level": "Full", "theme_system": "Custom" }

// C: ACCESSIBILITY_FOCUS
{ "preset": "ACCESSIBILITY_FOCUS", "accessibility": "Custom", "update_frequency": "Event-driven" }

// D: RESOURCE_EFFICIENT
{ "preset": "RESOURCE_EFFICIENT", "resource_monitoring": "Polling", "animation_level": "Basic" }
```

## Device Deployment

### Xiaomi Pad Deployment
- **Target Device**: Xiaomi Pad 6 (Android 13+)
- **Screen**: 11" 2560x1600 (WQXGA)
- **Touch**: Multi-touch support, stylus compatibility
- **Testing**: UI responsiveness, touch accuracy, resource monitoring

### iPad Deployment
- **Target Device**: iPad Pro (iOS 16+)
- **Screen**: 12.9" 2732x2048 (Retina)
- **Touch**: Multi-touch, Apple Pencil support
- **Testing**: iOS-specific UI patterns, accessibility compliance

## Release

### Android Release Pipeline
1. **WebView Version**: Chrome WebView 120+ compatibility
2. **Permissions**: POST_NOTIFICATIONS, FOREGROUND_SERVICE_DATA_SYNC
3. **Testing**: UI automation tests, accessibility validation
4. **Deployment**: APK with embedded WebView assets

### iOS Release Pipeline
1. **WKWebView**: iOS 16+ WebKit integration
2. **Capabilities**: Background processing, accessibility
3. **Testing**: XCTest UI automation, accessibility audit
4. **Deployment**: App Store with WebView optimization

### macOS Web Version
1. **Browser Support**: Chrome, Safari, Firefox compatibility
2. **Responsive Design**: Desktop and tablet layouts
3. **Testing**: Cross-browser validation, responsive design tests
4. **Deployment**: Static hosting with progressive web app features

## Scripts

See `scripts/` folder for:
- `test_ui_automation.sh` - UI interaction testing
- `test_accessibility.sh` - Accessibility compliance validation
- `test_webview_bridge.sh` - JavaScript bridge testing
- `test_resource_monitoring.sh` - Real-time resource monitoring
- `test_responsive_design.sh` - Cross-device layout validation