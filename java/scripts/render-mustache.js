#!/usr/local/bin/node

// If you pass "foo.html", this reads the template from "foo.html" and renders it
// with "foo.json" using Mustache.
//
// Before running: npm install -g mustache

var mustache = require('/usr/local/lib/node_modules/mustache');
var fs = require('fs')

// For compatibility with Java.
Array.prototype.size = Array.prototype.length;

function render(templateFile) {
  let template = fs.readFileSync(templateFile, 'utf-8');
  let jsonFile = templateFile.replace('.html', '.json');
  let jsonString = fs.readFileSync(jsonFile, 'utf-8')
  let json = JSON.parse(jsonString);
  return mustache.to_html(template, json);
}

if (require.main === module) {
  console.log(render(process.argv[2]));
} else {
  module.exports = {
    render: render
  };
}