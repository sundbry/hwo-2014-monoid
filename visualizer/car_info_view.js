goog.provide('monoid.CarInfoView');

goog.require('goog.ui.Component');
goog.require('monoid.Car');

goog.scope(function() {



/**
 * @param {monoid.Car} car
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.CarInfoView = function(car) {
  goog.base(this);

  /** @type {monoid.Car} */
  this.car_ = car;

  /** @type {Element} */
  this.slipAngleValue_ = null;

  /** @type {Element} */
  this.speedValue_ = null;

  /** @type {Element} */
  this.accelValue_ = null;
};
var CarInfoView = monoid.CarInfoView;
goog.inherits(CarInfoView, goog.ui.Component);


/** @override */
CarInfoView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var element = this.getElement();

  this.slipAngleValue_ = this.createFieldDom_('Slip angle:');
  this.speedValue_ = this.createFieldDom_('Speed:');
  this.accelValue_ = this.createFieldDom_('Acceleration:');
};


/**
 * @param {string} labelText
 * @private
 */
CarInfoView.prototype.createFieldDom_ = function(labelText) {
  var label = this.dom_.createDom('div', 'car-info-label');
  this.dom_.setTextContent(label, labelText);
  this.dom_.appendChild(this.getElement(), label);

  var valueDiv = this.dom_.createDom('div', 'car-info-value');
  this.dom_.appendChild(this.getElement(), valueDiv);
  return valueDiv;
};


/**
 * @param {number} gameTick
 */
CarInfoView.prototype.setGameTick = function(gameTick) {
  this.dom_.setTextContent(this.slipAngleValue_,
      this.car_.getPositionAtTick(gameTick).angle);
  this.dom_.setTextContent(this.speedValue_, this.car_.getSpeedAtTick(gameTick));
  this.dom_.setTextContent(this.accelValue_, this.car_.getAccelAtTick(gameTick));
};
});
