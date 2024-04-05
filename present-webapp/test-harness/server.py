#!/usr/local/bin/python2

import SimpleHTTPServer
import SocketServer
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import os
import pystache
import json

json = json.load(open('server.json'))

class MustacheHandler(BaseHTTPRequestHandler):
  
  def do_GET(self):
    print("doing get: "+self.path)
    try:
      if self.path.endswith('.html'):
        file = open('./'+self.path)
        html = file.read()

        self.send_response(200)
        self.send_header('Content-type','text-html')
        self.end_headers()

        result = pystache.render(html, json)

        self.wfile.write(result)
        file.close()
        return
      
    except IOError:
      self.send_error(404, 'file not found')

PORT = 8001
httpd = SocketServer.TCPServer(("", PORT), MustacheHandler)
print "serving at port", PORT


httpd.serve_forever()

