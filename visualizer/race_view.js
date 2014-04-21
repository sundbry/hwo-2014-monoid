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

  /** @type {Element} */
  this.slider_ = null;

  /** @type {number} */
  this.gameTick_ = 0;
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

  this.slider_ = this.dom_.createDom('input', {type: 'range', min: '0', max: '0', step: '1'});
  goog.dom.classlist.add(this.slider_, 'game-tick-slider');
  this.dom_.appendChild(element, this.slider_);
};


RaceView.prototype.draw = function() {
  this.map_.draw(this.gameTick_);
};


/**
 * @param {monoid.Race} race
 */
RaceView.prototype.setRace = function(race) {
  this.race_ = race;
  this.map_.setRace(race);
  this.slider_.max = race.getTotalGameTicks();
  this.draw();
};
});
