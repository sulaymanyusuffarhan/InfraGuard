import json
import socket
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from backend.monitor import Monitor, handler_for, load_targets, probe


class MonitorTests(unittest.TestCase):
    def test_rejects_bad_targets(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "targets.json"
            for targets in [[], [{"id": "a", "name": "bad", "address": "example.com", "port": 80}],
                            [{"id": "a", "name": "bad", "address": "127.0.0.1", "port": 0}]]:
                path.write_text(json.dumps(targets))
                with self.assertRaises(ValueError):
                    load_targets(path)

    def test_tcp_probe_and_authenticated_api(self):
        listener = socket.socket()
        listener.bind(("127.0.0.1", 0))
        listener.listen(1)
        target = {"id": "local", "name": "Test listener", "address": "127.0.0.1", "port": listener.getsockname()[1]}
        try:
            reading = probe(target)
            self.assertEqual(reading["status"], "Online")
            self.assertIsNotNone(reading["latencyMs"])
            monitor = Monitor([target], 30)
            monitor.snapshot = [reading]
            server = ThreadingHTTPServer(("127.0.0.1", 0), handler_for(monitor, "test-secret"))
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            url = f"http://127.0.0.1:{server.server_address[1]}/v1/monitoring/devices"
            try:
                with self.assertRaises(HTTPError) as error:
                    urlopen(url)
                self.assertEqual(error.exception.code, 401)
                request = Request(url, headers={"Authorization": "Bearer test-secret"})
                with urlopen(request) as response:
                    self.assertEqual(json.load(response)["devices"][0]["status"], "Online")
            finally:
                server.shutdown()
                server.server_close()
        finally:
            listener.close()


if __name__ == "__main__":
    unittest.main()
