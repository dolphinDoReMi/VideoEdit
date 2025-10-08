# Cursor Rules for VideoEditor Project

## Project Overview
This is a multi-platform video processing application combining CLIP video clipping and Whisper audio transcription across Android, iOS, and macOS web platforms.

## Code Style and Standards

### Kotlin/Android
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Prefer immutable data structures
- Use coroutines for asynchronous operations
- Follow Android architecture guidelines (MVVM)

### TypeScript/Web
- Use TypeScript strict mode
- Follow ESLint configuration
- Use meaningful variable and function names
- Prefer functional programming patterns
- Use modern ES6+ features

### Documentation
- Write comprehensive JSDoc/KDoc comments
- Update README files for new features
- Document all public APIs
- Include usage examples

## Architecture Guidelines

### Service Architecture
- Use foreground services for long-running operations
- Implement proper service lifecycle management
- Use WorkManager for background tasks
- Implement proper error handling and recovery

### Data Flow
- Use unidirectional data flow
- Implement proper state management
- Use dependency injection where appropriate
- Follow reactive programming patterns

### Resource Management
- Implement proper memory management
- Use resource pooling where appropriate
- Monitor resource usage
- Implement proper cleanup

## Testing Requirements

### Unit Tests
- Write unit tests for all new features
- Aim for 80%+ code coverage
- Use mock objects for dependencies
- Test error conditions and edge cases

### Integration Tests
- Write integration tests for service interactions
- Test cross-module integration
- Test device-specific functionality
- Test performance characteristics

### Device Tests
- Test on Xiaomi Pad 7 Ultra (primary Android target)
- Test on iPad (primary iOS target)
- Test various screen resolutions
- Test different hardware configurations

## Performance Guidelines

### Memory Management
- Monitor memory usage
- Implement proper garbage collection
- Use streaming processing for large files
- Implement memory limits and monitoring

### CPU Optimization
- Use appropriate thread counts
- Implement thermal management
- Use GPU acceleration where available
- Monitor CPU usage

### Battery Optimization
- Minimize background processing
- Use efficient algorithms
- Implement battery monitoring
- Optimize for mobile devices

## Security Guidelines

### Data Protection
- Use app-scoped storage
- Implement proper permission handling
- Encrypt sensitive data
- Follow security best practices

### Input Validation
- Validate all user inputs
- Sanitize file paths
- Check file formats
- Implement proper error handling

## Documentation Standards

### Code Documentation
- Document all public APIs
- Include usage examples
- Document configuration options
- Explain complex algorithms

### Architecture Documentation
- Document system architecture
- Explain design decisions
- Document control knots
- Include performance metrics

### User Documentation
- Write clear user guides
- Include troubleshooting sections
- Document known limitations
- Provide examples

## Git Workflow

### Branch Naming
- `feature/feature-name` for new features
- `bugfix/bug-description` for bug fixes
- `hotfix/critical-fix` for critical fixes
- `release/version-number` for releases

### Commit Messages
- Use conventional commit format
- Include descriptive messages
- Reference issues where appropriate
- Keep commits atomic

### Pull Requests
- Include comprehensive descriptions
- Reference related issues
- Include testing information
- Request appropriate reviewers

## Build and Deployment

### Android
- Use Gradle build system
- Follow Android build conventions
- Implement proper signing
- Use ProGuard for release builds

### iOS
- Use Xcode build system
- Follow iOS build conventions
- Implement proper code signing
- Use appropriate provisioning profiles

### Web
- Use modern build tools
- Implement proper bundling
- Use CDN for assets
- Implement proper caching

## Monitoring and Logging

### Logging
- Use appropriate log levels
- Include contextual information
- Implement log rotation
- Use structured logging

### Monitoring
- Monitor performance metrics
- Track error rates
- Monitor resource usage
- Implement alerting

### Analytics
- Track user behavior
- Monitor feature usage
- Track performance metrics
- Implement crash reporting

## Error Handling

### Error Types
- Network errors
- File system errors
- Processing errors
- User input errors

### Error Recovery
- Implement retry mechanisms
- Provide fallback options
- Graceful degradation
- User-friendly error messages

### Error Reporting
- Log detailed error information
- Include stack traces
- Report to monitoring systems
- Provide user feedback

## Platform-Specific Considerations

### Android
- Use Android-specific APIs
- Follow Material Design guidelines
- Implement proper lifecycle management
- Use Android-specific optimizations

### iOS
- Use iOS-specific APIs
- Follow Human Interface Guidelines
- Implement proper lifecycle management
- Use iOS-specific optimizations

### Web
- Use web-specific APIs
- Follow web accessibility guidelines
- Implement proper error handling
- Use web-specific optimizations

## Performance Targets

### CLIP Processing
- Frame processing: <0.1s per frame
- Memory usage: <200MB peak
- Accuracy: 95%+ on benchmarks
- Battery impact: <1% per hour

### Whisper Processing
- Processing speed: RTF <0.1
- Memory usage: <100MB peak
- Accuracy: 95%+ on benchmarks
- Battery impact: <2% per hour

### Infrastructure
- Hash calculation: <1ms per MB
- Storage savings: 40% reduction
- Error rate: <0.1%
- Background impact: <1% CPU

## Quality Assurance

### Code Review
- Review all code changes
- Check for security issues
- Verify performance impact
- Ensure proper testing

### Testing
- Run full test suite
- Test on target devices
- Verify performance metrics
- Check error handling

### Documentation
- Update documentation
- Verify accuracy
- Include examples
- Check completeness

## Maintenance

### Regular Tasks
- Update dependencies
- Review security updates
- Monitor performance
- Update documentation

### Monitoring
- Track performance metrics
- Monitor error rates
- Check resource usage
- Review user feedback

### Optimization
- Identify bottlenecks
- Optimize algorithms
- Improve resource usage
- Enhance user experience
