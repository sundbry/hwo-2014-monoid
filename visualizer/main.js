goog.provide('monoid.main');

goog.require('goog.dom');
goog.require('goog.debug.Console');
goog.require('monoid.LoadRaceButton');


goog.scope(function() {



monoid.main = function() {
  // Set up logging.
  goog.debug.Console.autoInstall();
  goog.debug.Console.instance.setCapturing(true);

  var loadRaceButton = new monoid.LoadRaceButton();
  loadRaceButton.render(goog.dom.getDocument().body);
};

goog.exportSymbol('monoid.main', monoid.main);
});  // goog.scope
