package com.miaad.iso8583TCPSocket;

public class IsoClientTest {
    public static void main(String[] args) {
        try {
            // Test connection
            System.out.println("Testing ISO-8583 client...");
            
            IsoConfig config = new IsoConfig.Builder("localhost", 8583)
                .connectTimeout(5000)
                .readTimeout(5000)
                .build();
            
            IsoClient client = new IsoClient(config);
            
            System.out.println("Attempting to connect...");
            client.connect();
            System.out.println("Connected!");
            
            // Test message
            byte[] testMessage = hexToBytes("0800822000000000000004000000000000001234");
            System.out.println("Sending test message...");
            
            IsoResponse response = client.sendAndReceive(testMessage);
            System.out.println("Response received!");
            System.out.println("Response time: " + response.getResponseTimeMs() + "ms");
            System.out.println("Response data: " + bytesToHex(response.getData()));
            
            client.close();
            System.out.println("Connection closed.");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
