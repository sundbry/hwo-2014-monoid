goog.provide('monoid.RaceMap');

goog.require('goog.ui.Component');

goog.scope(function() {



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.RaceMap = function() {
  goog.base(this);

  /** @type {CanvasRenderingContext2D} */
  this.context_ = null;

  /** @type {monoid.Race} */
  this.race_ = null;
};
var RaceMap = monoid.RaceMap;
goog.inherits(RaceMap, goog.ui.Component);


/** @override */
RaceMap.prototype.createDom = function() {
  this.setElementInternal(this.dom_.createElement('canvas'));
  this.context_ = this.getElement().getContext('2d');
};



RaceMap.prototype.draw = function() {
  var trackPieces = this.race_.getTrack().getPieces();
  var x = 10;
  var y = 10;
  for (var i = 0; i < trackPieces.length; i++) {
    this.context_.fillRect(x, y, 10, 10);
    x += 20;
    if (x > 250) { x = 10; y += 20;}
  }
};

/**
 * @param {monoid.Race} race
 */
RaceMap.prototype.setRace = function(race) {
  this.race_ = race;
};
});
