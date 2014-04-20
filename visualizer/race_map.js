goog.provide('monoid.RaceMap');

goog.require('goog.ui.Component');

goog.scope(function() {



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.RaceMap = function() {
  goog.base(this);

  /** @type {Element} */
  this.canvas_ = null;

  /** @type {monoid.Race} */
  this.race_ = null;
};
var RaceMap = monoid.RaceMap;
goog.inherits(RaceMap, goog.ui.Component);


/** @override */
RaceMap.prototype.createDom = function() {
  this.setElementInternal(this.dom_.createElement('canvas'));
};



RaceMap.prototype.draw = function() {

};

/**
 * @param {monoid.Race} race
 */
RaceMap.prototype.setRace = function(race) {
  this.race_ = race;
};
});
