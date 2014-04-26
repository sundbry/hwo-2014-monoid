goog.provide('monoid.RaceView');

goog.require('goog.dom.classlist');
goog.require('goog.events');
goog.require('goog.ui.Component');
goog.require('monoid.CarInfoView');
goog.require('monoid.RaceMap');
goog.require('monoid.RaceChart');

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

  /** @type {Array.<!monoid.CarInfoView>} */
  this.carInfoViews_ = [];

  /** @type {Element} */
  this.carInfosDiv_ = null;

  this.charts_ = [];
};
var RaceView = monoid.RaceView;
goog.inherits(RaceView, goog.ui.Component);


/** @override */
RaceView.prototype.createDom = function() {
  goog.base(this, 'createDom');

  var element = this.getElement();
  goog.dom.classlist.add(element, 'race-view');

  this.map_ = new monoid.RaceMap();
  this.registerDisposable(this.map_);
  this.map_.render(element);

  this.slider_ = this.dom_.createDom('input', {type: 'range', min: '0', max: '0', step: '1'});
  goog.dom.classlist.add(this.slider_, 'game-tick-slider');
  this.dom_.appendChild(element, this.slider_);
  this.getHandler().listen(this.slider_, goog.events.EventType.INPUT,
                           this.handleSliderChanged_);

  this.carInfosDiv_ = this.dom_.createDom('div', 'car-info-container');
  this.dom_.appendChild(element, this.carInfosDiv_);
};


/**
 * @param {number} gameTick
 */
RaceView.prototype.setGameTick = function(gameTick) {
  if (this.race_.getTrack() == null) {
    throw new Error("Track not loaded");
  }
  this.gameTick_ = gameTick;
  this.slider_.value = gameTick;
  this.map_.draw(this.gameTick_);
  for (var i = 0; i < this.carInfoViews_.length; i++) {
    this.carInfoViews_[i].setGameTick(gameTick);
  }
  /*
  for (var i = 0; i < this.charts_.length; i++) {
    this.charts_[i].draw(this.gameTick_);
  }
  */
};


/**
 * @param {goog.events.BrowserEvent} e
 */
RaceView.prototype.handleSliderChanged_ = function(e) {
  this.setGameTick(this.slider_.value);
};


/**
 * @param {monoid.Race} race
 */
RaceView.prototype.setRace = function(race) {
  this.race_ = race;
  this.map_.setRace(race);
  this.clearCarInfoViews_();
  var cars = race.getCars();
  for (var i = 0; i < cars.length; i++) {
    var view = new monoid.CarInfoView(cars[i]);
    view.render(this.carInfosDiv_);
    this.carInfoViews_.push(view);
  }
  this.slider_.max = race.getTotalGameTicks();
  this.loadRaceCharts(race);
  this.setGameTick(0);
};


/** @override */
RaceView.prototype.disposeInternal = function() {
  this.clearCarInfoViews_();
  goog.base(this, 'disposeInternal');
};


RaceView.prototype.clearCarInfoViews_ = function() {
  for (var i = 0; i < this.carInfoViews_.length; i++) {
    this.carInfoViews_[i].dispose();
  }
  this.carInfoViews_ = [];
};

RaceView.prototype.loadRaceCharts = function(race) {
	for (var i = 0; i < this.charts_.length; i++) {
		this.charts_[i].dispose();
	}
	this.charts_ = monoid.RaceChart.createDefaultCharts(race);
	for (var i = 0; i < this.charts_.length; i++) {
		this.charts_[i].render(this.getElement());
	}
};
});
