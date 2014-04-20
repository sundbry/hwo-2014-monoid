goog.provide('monoid.LoadRaceButton');

goog.require('goog.ui.Component');
goog.require('goog.dom.classlist');
goog.require('goog.fs');
goog.require('goog.fs.FileReader');
goog.require('monoid.Race');

goog.scope(function(){



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.LoadRaceButton = function() {
  goog.base(this);

  /** @private {monoid.RaceView} */
  this.raceView_ = null;

  /** @private {goog.fs.FileReader} */
  this.fileReader_ = new goog.fs.FileReader();
  this.getHandler().listen(this.fileReader_, goog.fs.FileReader.EventType.LOAD,
                           this.handleLoad_);
  this.registerDisposable(this.fileReader_);
};
var LoadRaceButton = monoid.LoadRaceButton;
goog.inherits(LoadRaceButton, goog.ui.Component);


/** @override */
LoadRaceButton.prototype.createDom = function() {
  goog.base(this, 'createDom');

  var element = this.getElement();

  var label = this.dom_.createDom('div', 'load-race-button-label');
  this.dom_.setTextContent(label, 'Load race data:');
  this.dom_.appendChild(element, label);

  var button = this.dom_.createDom('input', {type: 'file'});
  goog.dom.classlist.add(button, 'load-race-button');
  this.dom_.appendChild(element, button);
  this.getHandler().listen(button, goog.events.EventType.CHANGE, this.handleInputChanged_);
};


/**
 * @param {monoid.RaceView} raceView
 */
LoadRaceButton.prototype.setRaceView = function(raceView) {
  this.raceView_ = raceView;
};


/**
 * @param {goog.events.BrowserEvent} e
 * @private
 */
LoadRaceButton.prototype.handleInputChanged_ = function(e) {
  this.fileReader_.readAsText(e.target.files[0]);
};


/**
 * @param {goog.fs.ProgressEvent} e
 * @private
 */
LoadRaceButton.prototype.handleLoad_ = function(e) {
  var file = this.fileReader_.getResult();
  var race = new monoid.Race(/** @type{string} */(file));
  this.raceView_.setRace(race);
};
});
