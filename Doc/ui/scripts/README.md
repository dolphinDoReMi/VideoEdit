# UI Scripts

This directory contains scripts for testing and validating UI functionality.

## Core Testing Scripts

### `test_ui_automation.sh`
- **Purpose**: UI interaction testing
- **Usage**: `./test_ui_automation.sh`
- **Tests**: Touch events, gesture recognition, navigation

### `test_accessibility.sh`
- **Purpose**: Accessibility compliance validation
- **Usage**: `./test_accessibility.sh`
- **Tests**: Screen reader support, keyboard navigation, WCAG compliance

### `test_webview_bridge.sh`
- **Purpose**: JavaScript bridge testing
- **Usage**: `./test_webview_bridge.sh`
- **Tests**: WebView integration, JavaScript bridge, native calls

### `test_resource_monitoring.sh`
- **Purpose**: Real-time resource monitoring
- **Usage**: `./test_resource_monitoring.sh`
- **Tests**: Background service, resource updates, UI responsiveness

### `test_responsive_design.sh`
- **Purpose**: Cross-device layout validation
- **Usage**: `./test_responsive_design.sh`
- **Tests**: Screen size adaptation, orientation changes, device compatibility

## Usage Guidelines

1. **Prerequisites**: Ensure app is installed and WebView is functional
2. **Device Testing**: Test on both Xiaomi Pad and iPad
3. **Accessibility**: Use screen reader for accessibility testing
4. **Performance**: Monitor UI responsiveness and resource usage
5. **Cross-Platform**: Validate behavior across different platforms

## Troubleshooting

- **WebView Issues**: Check Chrome WebView version and compatibility
- **JavaScript Bridge**: Verify @JavascriptInterface methods
- **Accessibility**: Test with screen reader enabled
- **Performance**: Profile UI thread and memory usage
- **Cross-Platform**: Test on target devices and browsers