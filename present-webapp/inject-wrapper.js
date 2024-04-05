var fs = require("fs");
var assets = JSON.parse(fs.readFileSync("build/asset-manifest.json", "utf-8"));
var wrapper = fs.readFileSync("public/wrapper.html", "utf-8");
wrapper = wrapper.replace("%MAIN_JS%", assets["main.js"])
  .replace("%MAIN_CSS%", assets["main.css"]);
wrapper = "<!-- GENERATED - DO NOT EDIT! -->\n" + wrapper;
fs.writeFileSync("build/wrapper.html", wrapper, "utf-8");
