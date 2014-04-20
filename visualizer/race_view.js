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

  /** @type {monoid.RaceMap} */
  this.map_ = null;
};
var RaceView = monoid.RaceView;
goog.inherits(RaceView, goog.ui.Component);


/** @override */
RaceView.prototype.createDom = function() {
  goog.base(this, 'createDom');

  var element = this.getElement();
  goog.dom.classlist.add(element, 'race-view');

  this.map_ = new monoid.RaceMap();
  this.map_.render(element);
};


RaceView.prototype.draw = function() {
  this.map_.draw();
};


/**
 * @param {monoid.Race} race
 */
RaceView.prototype.setRace = function(race) {
  this.race_ = race;
  this.map_.setRace(race);
  this.draw();
};
});
