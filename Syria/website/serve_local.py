"""Serves the website locally like nginx does on the server, for testing the shared-link landing page.

- /l/<anything>  -> l/index.html (see nginx-share-links.conf)
- /api/<path>    -> the local API (default http://127.0.0.1:3000/<path>), like the server's /api/ proxy
- everything else from this folder

Usage: python Syria/website/serve_local.py [--port 8090] [--api http://127.0.0.1:3000]
The Android emulator reaches it at http://10.0.2.2:8090/l/<code>/<name>.
"""

import argparse
import http.server
import os
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))


def make_handler(api: str):
    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=ROOT, **kwargs)

        def _proxy(self, method: str) -> None:
            length = int(self.headers.get("Content-Length") or 0)
            body = self.rfile.read(length) if length else None
            req = urllib.request.Request(api + self.path[len("/api"):], data=body, method=method)
            if self.headers.get("Content-Type"):
                req.add_header("Content-Type", self.headers["Content-Type"])
            try:
                with urllib.request.urlopen(req, timeout=10) as res:
                    status, data, ctype = res.status, res.read(), res.headers.get("Content-Type")
            except urllib.error.HTTPError as e:
                status, data, ctype = e.code, e.read(), e.headers.get("Content-Type")
            self.send_response(status)
            if ctype:
                self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def do_POST(self):  # noqa: N802 (http.server naming)
            if self.path.startswith("/api/"):
                self._proxy("POST")
            else:
                self.send_error(405)

        def do_GET(self):  # noqa: N802
            if self.path.startswith("/api/"):
                self._proxy("GET")
                return
            if self.path.startswith("/l/"):
                self.path = "/l/index.html"
            super().do_GET()

        def guess_type(self, path):
            if path.endswith("assetlinks.json"):
                return "application/json"
            return super().guess_type(path)

    return Handler


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--port", type=int, default=8090)
    parser.add_argument("--api", default="http://127.0.0.1:3000")
    args = parser.parse_args()
    server = http.server.ThreadingHTTPServer(("0.0.0.0", args.port), make_handler(args.api.rstrip("/")))
    print(f"Serving {ROOT} on http://localhost:{args.port} (API: {args.api})", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
