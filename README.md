# iso8583TCPSocket

A lightweight, high-performance ISO-8583 TCP socket library for Android with comprehensive status monitoring, dual engine support, and production-ready features.

## 🚀 Features

- **🔧 Dual Engine Support**: Both blocking and non-blocking (NIO) I/O engines
- **🔒 TLS/SSL Support**: Built-in TLS encryption with certificate pinning
- **🔄 Automatic Reconnection**: Configurable retry logic with exponential backoff
- **📊 Comprehensive Status Monitoring**: 40+ status checking methods
- **📡 Real-time State Management**: Detailed connection state monitoring
- **🛡️ Thread-Safe**: Designed for concurrent usage with operation protection
- **📱 API 21+ Support**: Compatible with Android 5.0 and above
- **⚡ Lightweight**: Minimal dependencies and memory footprint
- **🎯 Production-Ready**: Tested with real-world scenarios
 - **🔁 Auto-close Control**: Choose to auto-close after response or keep connection open
 - **🚀 Performance Tuning**: tcpNoDelay, keepAlive, buffer reuse, NIO select interval, and more

## 📦 Installation

### Gradle

Add the JitPack repository to your project's `build.gradle` file:

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Then add the dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Miaadrajabi:iso8583TCPSocketClient:1.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.Miaadrajabi</groupId>
    <artifactId>iso8583TCPSocketClient</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 🎯 Quick Start

### Basic Usage

```java
// Create configuration
IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
    .connectTimeout(30000)
    .readTimeout(30000)
    .autoCloseAfterResponse(true) // default: close after response
    .connectionMode(ConnectionMode.BLOCKING)
    .build();

// Create client
IsoClient client = new IsoClient(config, 2, ByteOrder.BIG_ENDIAN);

// Connect
client.connect();

// Send message
byte[] message = "Hello ISO-8583!".getBytes(StandardCharsets.UTF_8);
IsoResponse response = client.sendAndReceive(message);

// Process response
System.out.println("Response: " + new String(response.getData()));
System.out.println("Response time: " + response.getResponseTimeMs() + "ms");

// Clean up
client.close();
```

### With Retry Configuration

```java
// Create retry config
RetryConfig retryConfig = new RetryConfig.Builder()
    .maxRetries(3)
    .baseDelay(1000)
    .maxDelay(10000)
    .backoffMultiplier(2.0)
    .retryOnTimeout(true)
    .retryOnConnectionFailure(true)
    .build();

// Create configuration with retry
IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
    .connectTimeout(30000)
    .readTimeout(30000)
    .autoCloseAfterResponse(false) // keep connection open for multiple requests
    .retryConfig(retryConfig)
    .connectionMode(ConnectionMode.BLOCKING)
    .build();
```

### With Status Monitoring

```java
// Set connection state listener
client.setConnectionStateListener(new ConnectionStateListener() {
    @Override
    public void onStateChanged(ConnectionState oldState, ConnectionState newState, String details) {
        System.out.println("State: " + oldState + " → " + newState + " (" + details + ")");
    }
    
    @Override
    public void onError(Exception error, ConnectionState state, String context) {
        System.err.println("Error in " + state + ": " + error.getMessage());
    }
    
    // ... implement other methods as needed
});

// Check connection status
ConnectionStatus status = client.getConnectionStatus();
System.out.println("Connected: " + status.isConnected());
System.out.println("Can send: " + status.canSend());
System.out.println("Health: " + status.isHealthy());

// Performance status (applied via IsoConfig)
// - tcpNoDelay, keepAlive, buffer sizes, reuseBuffers, nioSelectInterval, enableHotPathLogs
```

### With Retry Callback

```java
// Set retry callback
client.setRetryCallback(new RetryCallback() {
    @Override
    public void onRetryAttempt(int attempt, long delayMs, Exception lastException) {
        System.out.println("Retry attempt " + attempt + " after " + delayMs + "ms");
    }
    
    @Override
    public void onSuccess(int attempts) {
        System.out.println("Success after " + attempts + " attempts");
    }
    
    @Override
    public void onAllAttemptsFailed(int attempts, Exception lastException) {
        System.err.println("All " + attempts + " attempts failed");
    }
});
```

## 📊 Status Checking Methods

The library provides comprehensive status checking with 40+ methods:

### Connection Status
```java
client.isConnected()      // Check if connected
client.isOpen()          // Check if connection is open
client.isClosed()        // Check if connection is closed
client.isConnecting()    // Check if currently connecting
client.isDisconnecting() // Check if currently disconnecting
```

### Operation Status
```java
client.isTransactionInProgress() // Check if transaction in progress
client.isOperationInProgress()   // Check if operation in progress
client.isCancelled()            // Check if operation was cancelled
client.isRetrying()             // Check if currently retrying
```

### Error Status
```java
client.hasError()        // Check if there's an error
client.isTimeout()       // Check if timed out
client.getLastError()    // Get last error exception
```

### Socket Status
```java
client.isReadable()      // Check if socket is readable
client.isWritable()      // Check if socket is writable
client.isSocketBound()   // Check if socket is bound
client.isSocketClosed()  // Check if socket is closed
```

### Utility Methods
```java
client.canConnect()      // Check if can connect
client.canSend()         // Check if can send
client.canDisconnect()   // Check if can disconnect
client.isHealthy()       // Check if connection is healthy
client.needsReconnection() // Check if needs reconnection
```

### Detailed Status Information
```java
ConnectionStatus status = client.getConnectionStatus();
System.out.println(status.toDetailedString());
// Output:
// === Connection Status ===
// State: CONNECTED
// Connected: true
// Open: true
// Closed: false
// Mode: BLOCKING
// Engine: Blocking I/O Engine
// Local: /192.168.1.100:54321
// Remote: /10.0.2.2:8080
// Duration: 1234ms
// Health: true
```

## 🔧 Advanced Configuration

### Connection Modes

#### Blocking Mode (Default)
```java
IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
    .connectionMode(ConnectionMode.BLOCKING)
    .performanceMode() // optional low-latency preset
    .build();
```

#### Non-blocking NIO Mode
```java
IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
    .connectionMode(ConnectionMode.NON_BLOCKING)
    .reuseBuffers(true)
    .nioSelectIntervalMs(10)
    .build();
```

### TLS Configuration
```java
IsoConfig config = new IsoConfig.Builder("secure.example.com", 443)
    .useTls(true)
    .build();
```

### Length Header Configuration
```java
// 2-byte length header (default)
IsoClient client = new IsoClient(config, 2, ByteOrder.BIG_ENDIAN);

// 4-byte length header
IsoClient client = new IsoClient(config, 4, ByteOrder.LITTLE_ENDIAN);
```

### Performance Options

```java
IsoConfig perf = new IsoConfig.Builder(host, port)
    .tcpNoDelay(true)
    .keepAlive(true)
    .sendBufferSize(64 * 1024)
    .receiveBufferSize(64 * 1024)
    .reuseBuffers(true)
    .maxMessageSizeBytes(8 * 1024)
    .nioSelectIntervalMs(10)
    .enableHotPathLogs(false)
    .build();
```
        ## 🧪 Testing & Sample App

### Sample Android App

The library includes a comprehensive sample Android app that demonstrates all features:

- **Connection Testing**: Test different connection modes
- **Status Monitoring**: Real-time status display with 40+ status methods
- **Thread Safety Testing**: Test concurrent operations
- **Retry Testing**: Test retry mechanisms with configurable delays
- **Server Delay Testing**: Test with configurable server response delays

To use the sample app:

1. Clone the repository
2. Open in Android Studio
3. Run the `sample` module
4. Use the UI to test all features

### Python Test Server

A Python test server is included for testing:

```bash
# Start server with default 2-second delay
python3 test_server.py

# Start server with custom delay
python3 test_server.py 10  # 10-second delay

# Use quick test script
python3 quick_server_test.py 5  # 5-second delay
```

## 📋 Requirements

- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 33+
- **Java**: 8 or higher
- **Gradle**: 6.7.1+
- **Android Gradle Plugin**: 4.1+

## 🏗️ Architecture

### Package Structure
```
com.miaad.iso8583TCPSocket/
├── IsoClient.java              # Main client class
├── IsoConfig.java              # Configuration builder
├── IsoResponse.java            # Response wrapper
├── ConnectionStatus.java       # Status information
├── ConnectionState.java        # State enumeration
├── ConnectionStateListener.java # State monitoring
├── RetryConfig.java            # Retry configuration
├── RetryCallback.java          # Retry monitoring
├── ConnectionMode.java         # Engine mode selection
└── engine/
    ├── ConnectionEngine.java   # Engine interface
    ├── BlockingEngine.java     # Blocking I/O engine
    └── NonBlockingEngine.java  # Non-blocking NIO engine
```

### Key Components

- **IsoClient**: Main facade class with dual engine support
- **ConnectionEngine**: Interface for different I/O engines
- **BlockingEngine**: Traditional blocking socket implementation
- **NonBlockingEngine**: NIO-based non-blocking implementation
- **ConnectionStatus**: Comprehensive status information
- **RetryConfig**: Configurable retry policies
- **ConnectionStateListener**: Real-time state monitoring

## 🔄 Version History

### v1.1.0 - 2025-08-31
- Added auto-close control via `IsoConfig.autoCloseAfterResponse(boolean)`
- Added performance options: `tcpNoDelay`, `keepAlive`, `sendBufferSize`, `receiveBufferSize`, `reuseBuffers`, `maxMessageSizeBytes`, `nioSelectIntervalMs`, `enableHotPathLogs`
- Added `performanceMode()` preset for low-latency configs
- Improved engines to reuse buffers and tune NIO selector

### v1.0.0 - 2025-08-28
- **🎉 Initial Release**: Complete ISO-8583 TCP client library
- **🔧 Dual Engine Support**: Blocking and non-blocking I/O engines
- **📊 40+ Status Methods**: Comprehensive status checking
- **🔄 Retry Mechanism**: Configurable retry policies
- **📡 State Monitoring**: Real-time connection state monitoring
- **🛡️ Thread-Safe**: Concurrent operation protection
- **📱 API 21+ Support**: Android 5.0 and above compatibility
- **🧪 Sample App**: Comprehensive testing application
- **🐍 Test Server**: Python server for testing
- **🔧 Fixed**: Dependency resolution issues
- **📝 Updated**: Repository name to iso8583TCPSocketClient

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/Miaadrajabi/IsoSocket/issues)
- **Documentation**: [CHANGELOG.md](CHANGELOG.md)
- **Sample App**: Run the included sample app for examples

## 🎯 Key Benefits

- **Lightweight**: Minimal dependencies and memory footprint
- **Thread-Safe**: Designed for concurrent usage
- **Flexible**: Support for both blocking and non-blocking modes
- **Robust**: Comprehensive error handling and retry mechanisms
- **Observable**: Detailed state monitoring and logging
- **Production-Ready**: Tested with real-world scenarios
};

TcpClient client = new TcpClient.Builder()
    .host("example.com")
    .port(8080)
    .stateListener(listener)
    .build();
```

## Configuration Options

### Connection Settings

- `host(String)`: Server hostname or IP address
- `port(int)`: Server port number
- `mode(ConnectionMode)`: BLOCKING or NON_BLOCKING
- `connectTimeoutMs(int)`: Connection timeout in milliseconds
- `readTimeoutMs(int)`: Read timeout in milliseconds
- `writeTimeoutMs(int)`: Write timeout in milliseconds

### TLS Settings

- `enableTls(boolean)`: Enable TLS encryption
- `verifyHostname(boolean)`: Verify server hostname
- `trustManager(X509TrustManager)`: Custom trust manager
- `pinnedSpkiSha256(List<String>)`: Certificate pinning
- `tlsProtocolVersions(List<String>)`: Supported TLS versions

### Reconnection Settings

- `autoReconnect(boolean)`: Enable automatic reconnection
- `connectMaxRetries(int)`: Maximum retry attempts
- `initialBackoffMs(long)`: Initial backoff delay
- `maxBackoffMs(long)`: Maximum backoff delay
- `jitterFactor(float)`: Jitter factor for backoff

### Performance Settings

- `tcpNoDelay(boolean)`: Enable TCP_NODELAY
- `keepAlive(boolean)`: Enable TCP keep-alive
- `maxInFlightRequests(int)`: Maximum concurrent requests
- `requestQueueCapacity(int)`: Request queue capacity

## Connection States

The library provides detailed connection state information:

- `IDLE`: Initial state
- `CONNECTING`: Attempting to connect
- `HANDSHAKING`: Performing TLS handshake
- `CONNECTED`: TCP connection established
- `READY`: Ready for data exchange
- `SENDING`: Sending data
- `RECEIVING`: Receiving data
- `BACKING_OFF`: Waiting before retry
- `DISCONNECTED`: Connection lost
- `CLOSED`: Connection closed
- `ERROR`: Error occurred

## Error Handling

The library categorizes connection errors:

- `TIMEOUT`: Connection or operation timeout
- `DNS`: DNS resolution failure
- `NETWORK_UNREACHABLE`: Network unreachable
- `CONNECTION_REFUSED`: Connection refused by server
- `HANDSHAKE_TIMEOUT`: TLS handshake timeout
- `TLS_UNVERIFIED`: TLS verification failed
- `TLS_PINNING_MISMATCH`: Certificate pinning mismatch

## Thread Safety

IsoSocket is designed to be thread-safe. Multiple threads can safely use the same `TcpClient` instance for sending and receiving data.

## Performance Considerations

- Use `NON_BLOCKING` mode for high-throughput applications
- Configure appropriate timeouts based on your network conditions
- Use connection pooling for multiple concurrent connections
- Monitor connection states for optimal performance

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Author

**Miaad Rajabi**
- Email: miaad.rajabi@gmail.com
- GitHub: [@Miaadrajabi](https://github.com/Miaadrajabi)
