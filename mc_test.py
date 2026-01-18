#!/usr/bin/env python3
"""Test Oraxen copper block and backpack mechanics via MC-CLI"""

import json
import socket
import sys
import time
import os

class MCClient:
    def __init__(self, host="localhost", port=25580, timeout=30.0):
        self.host = host
        self.port = port
        self.timeout = timeout
        self._socket = None
        self._request_id = 0
        self._buffer = ""

    def connect(self):
        self._socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._socket.settimeout(self.timeout)
        self._socket.connect((self.host, self.port))

    def disconnect(self):
        if self._socket:
            self._socket.close()
            self._socket = None

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, *args):
        self.disconnect()
        return False

    def _next_id(self):
        self._request_id += 1
        return str(self._request_id)

    def _send(self, command, params=None):
        request = {
            "id": self._next_id(),
            "command": command,
            "params": params or {}
        }
        request_json = json.dumps(request) + "\n"
        self._socket.sendall(request_json.encode("utf-8"))

        while "\n" not in self._buffer:
            chunk = self._socket.recv(4096).decode("utf-8")
            if not chunk:
                raise ConnectionError("Connection closed")
            self._buffer += chunk

        line, self._buffer = self._buffer.split("\n", 1)
        return json.loads(line)

    def status(self):
        return self._send("status")

    def interact(self, action, **kwargs):
        params = {"action": action}
        params.update(kwargs)
        return self._send("interact", params)

    def window(self, action, **kwargs):
        params = {"action": action}
        params.update(kwargs)
        return self._send("window", params)

    def chat_send(self, msg):
        return self._send("chat", {"action": "send", "message": msg})

    def screenshot(self, path, clean=False, settle_ms=200):
        return self._send("screenshot", {
            "path": path,
            "clean": clean,
            "settle_ms": settle_ms
        })

    def inventory(self):
        return self._send("inventory", {"action": "list"})

    def teleport(self, x, y, z):
        return self._send("teleport", {"x": x, "y": y, "z": z})

    def camera(self, yaw, pitch):
        return self._send("camera", {"yaw": yaw, "pitch": pitch})


def main():
    screenshots_dir = os.path.expanduser("~/oraxen_test")
    os.makedirs(screenshots_dir, exist_ok=True)

    print("Connecting to MC-CLI...")
    try:
        with MCClient() as mc:
            # Check status
            status = mc.status()
            print(f"Status: {json.dumps(status.get('data', {}), indent=2)}")

            if not status.get("data", {}).get("in_game"):
                print("Not in game!")
                return

            # Try to disable focus grab for background operation
            print("\nTrying to disable focus grab...")
            result = mc.window("focus_grab", enabled=False)
            print(f"Window focus_grab result: {json.dumps(result, indent=2)}")

            # Close any open screens
            print("\nClosing any open screens...")
            result = mc.window("close_screen")
            print(f"Close screen result: {json.dumps(result, indent=2)}")

            # Select hotbar slot 0 (custom_oak_stairs)
            print("\nSelecting hotbar slot 0 (custom stairs)...")
            result = mc.interact("select", slot=0)
            print(f"Select result: {json.dumps(result, indent=2)}")

            # Right-click on block to place stairs at 2, 100, 0
            print("\nPlacing custom stairs at 2, 99, 0 (face up)...")
            result = mc.interact("use_on_block", hand="main", x=2, y=99, z=0, face="up")
            print(f"Place result: {json.dumps(result, indent=2)}")

            # Take screenshot to verify
            time.sleep(0.5)
            print("\nTaking screenshot...")
            result = mc.screenshot(f"{screenshots_dir}/copper_placement_test.png", clean=True, settle_ms=500)
            print(f"Screenshot: {json.dumps(result, indent=2)}")

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
