#!/usr/local/bin/node

// If you request "foo.html", this server will read the template from "foo.html" and render it
// with "foo.json" using Mustache.
//
// Before running: npm install -g mustache

var mustache = require('/usr/local/lib/node_modules/mustache');
var http = require('http');
var fs = require('fs');
var path = require('path');
var render = require(path.resolve(__dirname, "./render-mustache.js" ));


var server = http.createServer(function (request, response) {
  let templatePath = request.url.substring(1);
  if (!fs.existsSync(templatePath)) {
    // Look in appengine-api resources directory.
    templatePath = __dirname + "/../appengine-api/src/main/resources/" + templatePath;
  }
  if (!fs.existsSync(templatePath)) {
    response.writeHead(404);
    response.end();
    return;
  }
  
  response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' })
  response.write(render.render(templatePath))
  response.end();
})

console.log("Listening on http://localhost:8000/...")
server.listen(8000)
