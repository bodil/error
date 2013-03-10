var fs = require("fs");
var vm = require("vm");

function readScript(path) {
  var code = fs.readFileSync(path, "utf-8"),
      match = (code.match(/^#!.*\n/));
  return match ? code.slice(match[0].length - 1) : code;
}

var path = process.argv[2],
    code = readScript(path),
    context = vm.createContext(global);

context.require = require;

vm.runInContext(code, context, path);
vm.runInContext("cljs.core.string_print = function(s) { require('util').print(s); };", context);
vm.runInContext("error.environment.in_repl = false;", context);
vm.runInContext("error.test.run_tests();", context);
