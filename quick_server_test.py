#!/usr/bin/env python3
"""
Quick test script for different server delays
"""
import subprocess
import sys
import time
import signal
import os

def start_server(delay_seconds):
    """Start server with specified delay"""
    print(f"🚀 Starting server with {delay_seconds}s delay...")
    
    # Kill any existing server
    try:
        subprocess.run(["lsof", "-ti:8080"], capture_output=True, text=True, check=True)
        subprocess.run(["lsof", "-ti:8080"], stdout=subprocess.PIPE, text=True, shell=True)
        result = subprocess.run("lsof -ti:8080 | xargs kill -9", shell=True, capture_output=True)
        if result.returncode == 0:
            print("✅ Killed existing server")
        time.sleep(1)
    except:
        pass
    
    # Start new server
    cmd = ["python3", "test_server.py", str(delay_seconds)]
    process = subprocess.Popen(cmd)
    
    # Wait a bit for server to start
    time.sleep(2)
    
    # Check if server is running
    try:
        result = subprocess.run(["lsof", "-ti:8080"], capture_output=True, text=True, check=True)
        if result.stdout.strip():
            print(f"✅ Server running on port 8080 with {delay_seconds}s delay")
            return process
        else:
            print("❌ Failed to start server")
            return None
    except:
        print("❌ Failed to start server")
        return None

def main():
    if len(sys.argv) != 2:
        print("Usage: python3 quick_server_test.py <delay_seconds>")
        print("Examples:")
        print("  python3 quick_server_test.py 1    # 1 second delay")
        print("  python3 quick_server_test.py 5    # 5 second delay") 
        print("  python3 quick_server_test.py 10   # 10 second delay")
        print("  python3 quick_server_test.py 30   # 30 second delay")
        sys.exit(1)
    
    try:
        delay = float(sys.argv[1])
    except ValueError:
        print(f"❌ Invalid delay: {sys.argv[1]}")
        sys.exit(1)
    
    process = start_server(delay)
    if process:
        try:
            print(f"🔄 Server running... Press Ctrl+C to stop")
            process.wait()
        except KeyboardInterrupt:
            print("\n🛑 Stopping server...")
            process.terminate()
            time.sleep(1)
            if process.poll() is None:
                process.kill()
            print("✅ Server stopped")

if __name__ == "__main__":
    main()
