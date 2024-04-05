#!/usr/local/bin/python2

import os
import pystache
import json

json = json.load(open('server.json'))
html = open('./test.html').read()
result = pystache.render(html, json)

print(result)

