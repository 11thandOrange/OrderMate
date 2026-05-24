#!/usr/bin/env python3
"""Simple HTTP server with SPA fallback support."""
import http.server
import os
from pathlib import Path
from urllib.parse import urlparse

class SPAHandler(http.server.SimpleHTTPRequestHandler):
    """Handler that serves index.html for SPA routes."""
    
    def __init__(self, *args, directory=None, **kwargs):
        self.spa_directory = directory or os.getcwd()
        super().__init__(*args, directory=directory, **kwargs)
    
    def do_GET(self):
        # Parse the path (remove query string)
        parsed = urlparse(self.path)
        clean_path = parsed.path
        
        # Get the requested file path
        path = self.translate_path(clean_path)
        
        # If it's a file that exists, serve it normally
        if os.path.isfile(path):
            return super().do_GET()
        
        # If it's a directory with index.html, serve that
        if os.path.isdir(path):
            index_path = os.path.join(path, 'index.html')
            if os.path.isfile(index_path):
                return super().do_GET()
        
        # Otherwise, serve index.html for SPA routing
        self.path = '/index.html'
        return super().do_GET()

if __name__ == '__main__':
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 12000
    
    # Change to dist directory
    dist_dir = Path(__file__).parent / 'dist'
    os.chdir(dist_dir)
    
    print(f"Serving SPA from {dist_dir} on port {port}")
    print(f"Open: http://localhost:{port}/")
    
    server = http.server.HTTPServer(('0.0.0.0', port), SPAHandler)
    server.serve_forever()
