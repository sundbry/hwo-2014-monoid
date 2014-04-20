goog.provide('monoid.Track');
goog.provide('monoid.TrackPiece');

goog.scope(function() {



/**
 * @param {TrackMessage} track
 * @constructor
 */
monoid.Track = function(track) {
  /** @private {string} */
  this.id_ = track.id;

  /** @private {string} */
  this.name_ = track.name;

  /** @private {Array.<TrackPiece>} */
  this.pieces_ = [];
  var lastPos = new Position(track.startingPoint);
  lastPos.x = 100;
  lastPos.y = 100;
  for (var i = 0; i < track.pieces.length; i++) {
    this.pieces_.push(new monoid.TrackPiece(track.pieces[i], lastPos));
    lastPos = this.pieces_[i].getEndPosition();
  }
};
var Track = monoid.Track;


/**
 * @returns {Array.<monoid.TrackPiece>}
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
};
var TrackPiece = monoid.TrackPiece;


/**
 * @returns {boolean}
 */
TrackPiece.prototype.isStraight = function() {
  return (this.length_ !== undefined) ? true : false;
};

/**
 * @returns {Position}
 */
TrackPiece.prototype.getEndPosition = function() {
  if (this.endPos_) return this.endPos_;
  this.endPos_ = new Position();

  if (this.isStraight()) {
    this.endPos_.x = this.startPos_.x + this.length_ * Math.sin(this.startPos_.angle);
    this.endPos_.y = this.startPos_.y + this.length_ * Math.cos(this.startPos_.angle);
    this.endPos_.angle = this.startPos_.angle;
  }

  return this.endPos_;
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
