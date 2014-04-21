goog.provide('monoid.Car');

goog.require('goog.log');

goog.scope(function() {



/**
 * @param {CarMessage} car
 * @constructor
 */
monoid.Car = function(car) {
  /** @type {Car.Id} */
  this.id_ = new Car.Id(car.id);

  /** @type {Array.<Position>} */
  this.positions_ = [];
};
var Car = monoid.Car;


Car.logger_ = goog.log.getLogger('monoid.Car');


/**
 * @param {CarPositionMessage} position
 * @param {number} gameTick
 * @param {!Array.<monoid.TrackPiece>} pieces
 */
Car.prototype.setPositionAt = function(position, gameTick, pieces) {
  this.positions_[gameTick] = new Position(position, pieces);
};


Car.prototype.getId = function() {
  return this.id_;
};



/**
 * @param {CarPositionMessage} position
 * @param {!Array.<monoid.TrackPiece>} pieces
 * @constructor
 */
Car.Position = function(position, pieces) {
  /** @type {number} */
  this.angle_ = position.angle;

  /** @type {monoid.TrackPiece} */
  this.piece_ = pieces[position.piecePosition.pieceIndex];

  /** @type {number} */
  this.distance_ = position.piecePosition.inPieceDistance;
};
var Position = Car.Position;



/**
 * @param {CarIdMessage} id
 * @constructor
 */
Car.Id = function(id) {
  /** @type {string} */
  this.name_ = id.name;

  /** @type {string} */
  this.color_ = id.color;
};


/**
 * @returns {string}
 */
Car.Id.prototype.toString = function() {
  return this.name_ + ' ' + this.color_;
};


/**
 * @param {Car.Id} other
 * @returns {boolean}
 */
Car.Id.prototype.equals = function(other) {
  return this.name_ == other.name_ && this.color_ == other.color_;
};
});
