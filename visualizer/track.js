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

  this.pieces_ = [];
  for (var i = 0; i < track.pieces.length; i++) {
    this.pieces_.push(new monoid.TrackPiece(track.pieces[i]));
  }
};


/** @constructor */
monoid.TrackPiece = function(piece) {
  /** @type {boolean} */
  this.straight_ = piece.length ? true : false;

  /** @type {number} */
  this.length_ = piece.length;

  /** @type {number} */
  this.radius_ = piece.radius;

  /** @type {number} */
  this.angle_ = piece.angle;

  /** @type {boolean} */
  this.switch_ = piece['switch'] ? true : false;
};
});
