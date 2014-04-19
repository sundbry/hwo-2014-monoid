goog.provide('monoid.main');

goog.require('goog.dom');
goog.require('monoid.LoadRaceButton');

goog.scope(function() {



monoid.main = function() {
  var loadRaceButton = new monoid.LoadRaceButton();
  loadRaceButton.render(goog.dom.getDocument().body);
};

goog.exportSymbol('monoid.main', monoid.main);
});  // goog.scope
