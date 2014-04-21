goog.provide('monoid.Track');
goog.provide('monoid.TrackPiece');

goog.scope(function() {


/**
 * @param {number} num
 * @returns {number}
 */
var signum = function(num) {
  return num > 0 ? 1 : -1;
};



/**
 * @param {TrackMessage} track
 * @constructor
 */
monoid.Track = function(track) {
  /** @private {string} */
  this.id_ = track.id;

  /** @private {string} */
  this.name_ = track.name;

  /** @private {!Array.<TrackPiece>} */
  this.pieces_ = [];
  var lastPos = new Position(track.startingPoint);
  for (var i = 0; i < track.pieces.length; i++) {
    this.pieces_.push(new monoid.TrackPiece(track.pieces[i], lastPos));
    lastPos = this.pieces_[i].getEndPosition();
  }
};
var Track = monoid.Track;


/**
 * Calculate the minimum coordinates that need to be visible to view the entire
 * track.
 * @returns {{x: number, y: number, w: number, h: number}}
 */
Track.prototype.getDimensions = function() {
  var pos = this.pieces_[0].getStartPosition();
  var minX = pos.x;
  var maxX = pos.x;
  var minY = pos.y;
  var maxY = pos.y;
  for (var i = 0; i < this.pieces_.length; i++) {
    var piece = this.pieces_[i];
    if (piece.isStraight()) {
      var start = piece.getStartPosition();
      var end = piece.getEndPosition();
      minX = Math.min(minX, Math.min(start.x, end.x));
      maxX = Math.max(maxX, Math.max(start.x, end.x));
      minY = Math.min(minY, Math.min(start.y, end.y));
      maxY = Math.max(maxY, Math.max(start.y, end.y));
    } else {
      var center = piece.getCenterPosition();
      minX = Math.min(minX, center.x - piece.getRadius());
      maxX = Math.max(maxX, center.x + piece.getRadius());
      minY = Math.min(minY, center.y - piece.getRadius());
      maxY = Math.max(maxY, center.y + piece.getRadius());
    }
  }
  return {x: minX, y: minY, w: maxX - minX, h: maxY - minY};
};


/**
 * @returns {!Array.<monoid.TrackPiece>}
 */
Track.prototype.getPieces = function() {
  return this.pieces_;
};

/**
 * @param {PieceMessage} piece
 * @param {Position} startPos
 * @constructor
 */
monoid.TrackPiece = function(piece, startPos) {
  /** @type {number} */
  this.length_ = piece.length;

  /** @type {number} */
  this.radius_ = piece.radius;

  /** @type {number} */
  this.bendAngle_ = piece.angle * Math.PI / 180;

  /** @type {boolean} */
  this.switch_ = piece['switch'] ? true : false;

  /** @type {Position} */
  this.startPos_ = startPos;

  /** @type {Position} */
  this.endPos_ = null;

  /** @type {Position} */
  this.centerPos_ = null;
};
var TrackPiece = monoid.TrackPiece;


/**
 * @returns {boolean}
 */
TrackPiece.prototype.isStraight = function() {
  return (this.length_ !== undefined) ? true : false;
};


/**
 * @returns {number}
 */
TrackPiece.prototype.getRadius = function() {
  return this.radius_;
};


/**
 * @returns {number}
 */
TrackPiece.prototype.getBendAngle = function() {
  return this.bendAngle_;
};


/**
 * @returns {Position}
 */
TrackPiece.prototype.getEndPosition = function() {
  if (this.endPos_) return this.endPos_;
  this.endPos_ = new Position();

  if (this.isStraight()) {
    this.endPos_.x = this.startPos_.x + this.length_ * Math.sin(this.startPos_.angle);
    this.endPos_.y = this.startPos_.y - this.length_ * Math.cos(this.startPos_.angle);
    this.endPos_.angle = this.startPos_.angle;
  } else {
    var center = this.getCenterPosition();
    var angle = center.angle + this.bendAngle_;
    this.endPos_.x = center.x + this.radius_ * Math.sin(angle);
    this.endPos_.y = center.y - this.radius_ * Math.cos(angle);
    this.endPos_.angle = this.startPos_.angle + this.bendAngle_;
  }

  return this.endPos_;
};


/**
 * The angle of the center position is the angle from it to the start position.
 * @returns {Position}
 */
TrackPiece.prototype.getCenterPosition = function() {
  if (this.isStraight()) return null;
  if (this.centerPos_ != null) return this.centerPos_;
  this.centerPos_ = new Position();

  this.centerPos_.angle = this.startPos_.angle + signum(this.bendAngle_)*Math.PI/2 + Math.PI;
  this.centerPos_.x = this.startPos_.x - this.radius_ * Math.sin(this.centerPos_.angle);
  this.centerPos_.y = this.startPos_.y + this.radius_ * Math.cos(this.centerPos_.angle);
  return this.centerPos_;
};


/**
 * @returns {Position}
 */
TrackPiece.prototype.getStartPosition = function() {
  return this.startPos_;
};


/**
 * @struct
 * @param {PiecePosition=} opt_position
 * @constructor
 */
TrackPiece.Position = function(opt_position) {
  /** @type {number} */
  this.x = opt_position ? opt_position.position.x : 0;
  this.y = opt_position ? opt_position.position.y : 0;
  this.angle = opt_position ? (opt_position.angle * Math.PI / 180) : 0;
};
var Position = TrackPiece.Position;
});
