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

  this.scale_ = 0.3;
};
var RaceMap = monoid.RaceMap;
goog.inherits(RaceMap, goog.ui.Component);


/** @override */
RaceMap.prototype.createDom = function() {
  this.setElementInternal(this.dom_.createDom('canvas', {width: 800, height: 400}));
  this.context_ = this.getElement().getContext('2d');
};


RaceMap.prototype.draw = function() {
  this.drawTrack_();
};


RaceMap.prototype.drawTrack_ = function() {
  var trackPieces = this.race_.getTrack().getPieces();
  for (var i = 0; i < trackPieces.length; i++) {
    this.drawTrackPiece_(trackPieces[i]);
  }
};


/**
 * @param {monoid.TrackPiece} piece
 */
RaceMap.prototype.drawTrackPiece_ = function(piece) {
  if (piece.isStraight()) {
    var startPos = piece.getStartPosition();
    var endPos = piece.getEndPosition();
    this.context_.beginPath();
    this.context_.moveTo(this.scale_ * startPos.x, this.scale_ * startPos.y);
    this.context_.lineTo(this.scale_ * endPos.x, this.scale_ * endPos.y);
    this.context_.stroke();
  } else {
    var center = piece.getCenterPosition();
    this.context_.beginPath();
    this.context_.arc(this.scale_ * center.x, this.scale_ * center.y, this.scale_ * piece.getRadius(),
        center.angle - Math.PI/2, center.angle + piece.getBendAngle() - Math.PI/2);
    this.context_.stroke();
  }
};


/**
 * @param {monoid.Race} race
 */
RaceMap.prototype.setRace = function(race) {
  this.race_ = race;
};
});
