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

  /** @type {number} */
  this.scale_ = 1;

  /** @type {number} */
  this.translateX_ = 0;

  /** @type {number} */
  this.translateY_ = 0;
};
var RaceMap = monoid.RaceMap;
goog.inherits(RaceMap, goog.ui.Component);


/** @const {number} */
RaceMap.WIDTH = 800;

/** @const {number} */
RaceMap.HEIGHT = 400;


/** @override */
RaceMap.prototype.createDom = function() {
  this.setElementInternal(
      this.dom_.createDom('canvas',
                          {width: RaceMap.WIDTH, height: RaceMap.HEIGHT}));
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
    this.context_.moveTo(startPos.x, startPos.y);
    this.context_.lineTo(endPos.x, endPos.y);
    this.context_.stroke();
  } else {
    var center = piece.getCenterPosition();
    this.context_.beginPath();
    this.context_.arc(
        center.x, center.y, piece.getRadius(),
        center.angle - Math.PI/2,
        center.angle + piece.getBendAngle() - Math.PI/2,
        piece.getBendAngle() < 0);
    this.context_.stroke();
  }
};


/**
 * @param {monoid.Race} race
 */
RaceMap.prototype.setRace = function(race) {
  this.race_ = race;

  // Translate and scale so we can fit the track nicely.
  var dimensions = race.getTrack().getDimensions();
  var scaleWidth = RaceMap.WIDTH / dimensions.w;
  var scaleHeight = RaceMap.HEIGHT / dimensions.h;
  var scaleFactor = Math.min(scaleWidth, scaleHeight);
  this.context_.scale(scaleFactor / this.scale_, scaleFactor / this.scale_);
  this.scale_ = scaleFactor;

  var transX = -dimensions.x + dimensions.w/2 * (scaleWidth/scaleFactor - 1);
  var transY = -dimensions.y + dimensions.h/2 * (scaleHeight/scaleFactor - 1);
  this.context_.translate(transX - this.translateX_,
                          transY - this.translateY_);
  this.translateX_ = transX;
  this.translateY_ = transY;
};
});
