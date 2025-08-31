#!/usr/bin/env python3
import socket
import struct
import threading
import time
import sys

# Configurable delay (seconds)
RESPONSE_DELAY = 1.0  # Default 10 seconds

def handle_client(client_socket, addr):
    print(f"Client connected from {addr}")
    try:
        # Set socket timeout
        client_socket.settimeout(30.0)
        
        # Read length header (2 bytes, big endian)
        print("Waiting for length header...")
        header = client_socket.recv(2)
        if len(header) < 2:
            print("Failed to read header")
            return
            
        length = struct.unpack('>H', header)[0]
        print(f"Message length: {length}")
        print(f"Header bytes: {header.hex()}")
        
        # Read the actual message
        data = b''
        while len(data) < length:
            chunk = client_socket.recv(length - len(data))
            if not chunk:
                print("Connection closed while reading data")
                return
            data += chunk
            print(f"Read {len(chunk)} bytes, total: {len(data)}/{length}")
            
        print(f"Received: {data.hex()}")
        
        # Simulate processing delay (like a real database query)
        print(f"Simulating database query/processing... (will take {RESPONSE_DELAY} seconds)")
        
        # Show countdown for long delays
        if RESPONSE_DELAY > 3:
            for i in range(int(RESPONSE_DELAY), 0, -1):
                print(f"Processing... {i} seconds remaining")
                time.sleep(1.0)
            # Handle fractional part
            remaining = RESPONSE_DELAY - int(RESPONSE_DELAY)
            if remaining > 0:
                time.sleep(remaining)
        else:
            time.sleep(RESPONSE_DELAY)
            
        print("Processing completed, sending response...")
        
        # Send echo response
        response = data  # Echo back the same data
        response_header = struct.pack('>H', len(response))
        
        print(f"Sending response header: {response_header.hex()}")
        print(f"Sending response data: {response.hex()}")
        
        # Send response in one go
        full_response = response_header + response
        client_socket.send(full_response)
        print(f"Response sent successfully ({len(full_response)} bytes)")
        
        # Keep connection alive for a bit
        time.sleep(0.1)
        
    except socket.timeout:
        print("Client timeout")
    except Exception as e:
        print(f"Error handling client: {e}")
        import traceback
        traceback.print_exc()
    finally:
        try:
            client_socket.close()
        except:
            pass
        print(f"Client {addr} disconnected")

def main():
    global RESPONSE_DELAY
    
    # Check for command line argument to set delay
    if len(sys.argv) > 1:
        try:
            RESPONSE_DELAY = float(sys.argv[1])
            print(f"Using custom response delay: {RESPONSE_DELAY} seconds")
        except ValueError:
            print(f"Invalid delay value '{sys.argv[1]}', using default {RESPONSE_DELAY} seconds")
    
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('0.0.0.0', 8080))
    server.listen(5)
    
    print("="*50)
    print("🚀 ISO-8583 Test Server")
    print("="*50)
    print(f"📡 Listening on: 0.0.0.0:8080")
    print(f"⏱️  Response delay: {RESPONSE_DELAY} seconds")
    print(f"💡 Usage: python3 test_server.py [delay_seconds]")
    print("="*50)
    print("Waiting for connections...")
    
    try:
        while True:
            client, addr = server.accept()
            thread = threading.Thread(target=handle_client, args=(client, addr))
            thread.daemon = True
            thread.start()
    except KeyboardInterrupt:
        print("\nShutting down server...")
    finally:
        server.close()

if __name__ == "__main__":
    main()
