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


/**
 * @returns {string} color
 */
Car.prototype.getColor = function() {
  return this.id_.color_;
};


/**
 * @param {number} tick
 * @returns {Car.Position}
 */
Car.prototype.getPositionAtTick = function(tick) {
  return this.positions_[tick];
};


/**
 * @returns {Car.Id}
 */
Car.prototype.getId = function() {
  return this.id_;
};



/**
 * @param {CarPositionMessage} position
 * @param {!Array.<monoid.TrackPiece>} pieces
 * @struct
 * @constructor
 */
Car.Position = function(position, pieces) {
  /** @type {number} */
  this.angle = position.angle;

  /** @type {number} */
  this.x = 0;

  /** @type {number} */
  this.y = 0;

  var piece = pieces[position.piecePosition.pieceIndex];
  var dist = position.piecePosition.inPieceDistance;

  if (piece.isStraight()) {
    var pos = piece.getStartPosition();
    this.x = pos.x + dist * Math.sin(pos.angle);
    this.y = pos.y - dist * Math.cos(pos.angle);
  }
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
