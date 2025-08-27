# iso8583TCPSocket

A lightweight, high-performance ISO-8583 TCP socket library for Android with support for both blocking and non-blocking I/O, TLS encryption, and automatic reconnection.

## Features

- **Dual Engine Support**: Both blocking and non-blocking (NIO) I/O engines
- **TLS/SSL Support**: Built-in TLS encryption with certificate pinning
- **Automatic Reconnection**: Configurable retry logic with exponential backoff
- **Connection State Management**: Real-time connection state monitoring
- **API 21+ Support**: Compatible with Android 5.0 and above
- **Lightweight**: Minimal dependencies and memory footprint
- **Thread-Safe**: Designed for concurrent usage

## Installation

### Gradle

Add the following to your `build.gradle` file:

```gradle
dependencies {
    implementation 'com.github.Miaadrajabi:iso8583TCPSocket:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.Miaadrajabi</groupId>
    <artifactId>iso8583TCPSocket</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
TcpClient client = new TcpClient.Builder()
    .host("example.com")
    .port(8080)
    .build();

try {
    client.connect();
    
    byte[] request = "Hello, Server!".getBytes();
    TcpResponse response = client.sendAndReceive(request, 5000);
    
    System.out.println("Response: " + new String(response.getPayload()));
} catch (IOException | TimeoutException e) {
    e.printStackTrace();
} finally {
    client.close();
}
```

### With TLS Support

```java
TcpClient client = new TcpClient.Builder()
    .host("secure.example.com")
    .port(443)
    .enableTls(true)
    .verifyHostname(true)
    .build();
```

### With Auto-Reconnection

```java
TcpClient client = new TcpClient.Builder()
    .host("example.com")
    .port(8080)
    .autoReconnect(true)
    .connectMaxRetries(5)
    .initialBackoffMs(1000)
    .maxBackoffMs(30000)
    .build();
```

### State Monitoring

```java
StateListener listener = new StateListener() {
    @Override
    public void onStateChanged(ConnectionState state, StateInfo info) {
        System.out.println("Connection state: " + state);
    }
    
    @Override
    public void onTraffic(TrafficEvent event) {
        System.out.println("Traffic: " + event.kind + " - " + event.byteCount + " bytes");
    }
    
    @Override
    public void onError(Throwable t, ErrorStage stage) {
        System.err.println("Error in " + stage + ": " + t.getMessage());
    }
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
