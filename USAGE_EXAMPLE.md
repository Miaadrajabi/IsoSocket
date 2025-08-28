# IsoSocket Library Usage Example

## Installation

### Step 1: Add JitPack Repository

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

### Step 2: Add Dependency

Add the dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.Miaadrajabi:iso8583TCPSocket:1.0.0'
}
```

## Basic Usage Example

```java
import com.miaad.iso8583TCPSocket.*;

public class IsoSocketExample {
    
    public void connectToServer() {
        try {
            // Create configuration
            IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
                .connectTimeout(30000)
                .readTimeout(30000)
                .connectionMode(ConnectionMode.BLOCKING)
                .build();

            // Create client
            IsoClient client = new IsoClient(config, 2, ByteOrder.BIG_ENDIAN);

            // Connect to server
            client.connect();

            // Send message
            byte[] message = "Hello ISO-8583!".getBytes(StandardCharsets.UTF_8);
            IsoResponse response = client.sendAndReceive(message);

            // Process response
            System.out.println("Response: " + new String(response.getData()));
            System.out.println("Response time: " + response.getResponseTimeMs() + "ms");

            // Clean up
            client.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Advanced Usage with Retry Configuration

```java
public class AdvancedIsoSocketExample {
    
    public void connectWithRetry() {
        try {
            // Create retry configuration
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
                .retryConfig(retryConfig)
                .connectionMode(ConnectionMode.BLOCKING)
                .build();

            // Create client
            IsoClient client = new IsoClient(config, 2, ByteOrder.BIG_ENDIAN);

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
            });

            // Connect and send message
            client.connect();
            
            byte[] message = "Test message".getBytes(StandardCharsets.UTF_8);
            IsoResponse response = client.sendAndReceive(message);
            
            System.out.println("Success: " + new String(response.getData()));
            
            client.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Status Monitoring Example

```java
public class StatusMonitoringExample {
    
    public void monitorConnection() {
        try {
            IsoConfig config = new IsoConfig.Builder("192.168.1.100", 8583)
                .connectTimeout(30000)
                .readTimeout(30000)
                .build();

            IsoClient client = new IsoClient(config, 2, ByteOrder.BIG_ENDIAN);
            client.connect();

            // Check various status
            System.out.println("Connected: " + client.isConnected());
            System.out.println("Can send: " + client.canSend());
            System.out.println("Is healthy: " + client.isHealthy());
            
            // Get detailed status
            ConnectionStatus status = client.getConnectionStatus();
            System.out.println(status.toDetailedString());
            
            client.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Troubleshooting

### Common Issues:

1. **Dependency Resolution Error**: Make sure you've added the JitPack repository
2. **Connection Timeout**: Check your server address and port
3. **Version Mismatch**: Use the exact version `1.0.0`

### Correct Dependency Format:

```gradle
// ✅ Correct
implementation 'com.github.Miaadrajabi:iso8583TCPSocket:1.0.0'

// ❌ Wrong
implementation 'com.github.Miaadrajabi:IsoSocket:1.0.0'
```

## Available Features

- ✅ Dual Engine Support (Blocking & Non-blocking)
- ✅ TLS/SSL Support
- ✅ Automatic Reconnection
- ✅ Comprehensive Status Monitoring (40+ methods)
- ✅ Real-time State Management
- ✅ Thread-Safe Operations
- ✅ Configurable Retry Policies
- ✅ Connection State Listeners
- ✅ Response Time Monitoring

## Support

For more information, see the main [README.md](README.md) file.
