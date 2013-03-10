var page = require("webpage").create(),
    sys = require("system"),
    path = sys.args[1];

page.onConsoleMessage = function(msg) {
  console.log(msg);
};

page.onCallback = function(msg) {
  if (msg.cmd === "write") {
    sys.stdout.write(msg.data);
    sys.stdout.flush();
  } else if (msg.cmd === "quit") {
    phantom.exit(msg.data);
  }
};

page.onLoadFinished = function() {
  page.injectJs(path);
  page.evaluate(function() {
    cljs.core.string_print = function(s) {
      window.callPhantom({cmd: "write", data: s});
    };
    error.test.run_tests("phantom");
  });
};

page.open("about:blank");
