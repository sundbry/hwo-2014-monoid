goog.provide('monoid.RaceView');

goog.require('goog.dom.classlist');
goog.require('goog.ui.Component');
goog.require('monoid.RaceMap');

goog.scope(function() {



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.RaceView = function() {
  goog.base(this);

  /** @type {monoid.Race} */
  this.race_ = null;
};
var RaceView = monoid.RaceView;
goog.inherits(RaceView, goog.ui.Component);


/** @override */
RaceView.prototype.createDom = function() {
  goog.base(this, 'createDom');

  var element = this.getElement();
  goog.dom.classlist.add(element, 'race-view');

  var map = new monoid.RaceMap();
  map.render(element);
};


/**
 * @param {monoid.Race} race
 */
RaceView.prototype.displayRace = function(race) {
  this.race_ = race;
  // TODO: Draw.
};
});
