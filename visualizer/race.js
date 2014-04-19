goog.provide('monoid.Race');

goog.require('goog.log');
goog.require('goog.log.Logger');
goog.require('monoid.Track');
goog.require('monoid.Car');

goog.scope(function() {



/**
 * @param {string} raceLog log of a single race
 * @constructor
 */
monoid.Race = function(raceLog) {
  /** @private {monoid.Track} */
  this.track_ = null;

  /** @private {number} */
  this.laps_ = 0;

  /** @private {number} */
  this.maxLapTime_ = 0;

  /** @private {boolean} */
  this.quickRace = false;

  /** @private {!Array<!monoid.Car>} */
  this.cars_ = [];

  this.parseLog_(raceLog.split('\n'));
};
var Race = monoid.Race;

/** @private goog.log.Logger */
Race.logger_ = new goog.log.getLogger('monoid.Race');

/**
 * @param {!Array.<string>} lines
 * @private
 */
Race.prototype.parseLog_ = function(lines) {
  for (var i = 0; i < lines.length; i++) {
    if (!lines[i]) continue;
    // Quick and dirty
    var line = eval(lines[i]);
    if (line[1] == 'in') {
      this.parseInput(line[2], line[0]);
    } else if (line[1] == 'out') {
      this.parseOutput(line[2], line[1]);
    }
  }
};


/**
 * @param {object} msg
 * @param {number} timestamp
 */
Race.prototype.parseInput = function(msg, timestamp) {
  switch (msg.msgType) {
   case 'gameInit':
    this.gameInit(msg.data.race, timestamp);
    break;
   default:
    goog.log.info(Race.logger_, 'Unknown message type: ' + msg.msgType, msg);
  }
};


Race.prototype.gameInit = function(race, timestamp) {
  this.track_ = new monoid.Track(race.track);
  for (var i = 0; i < race.cars.length; i++) {
    this.cars_.push(new monoid.Car(race.cars[i]));
  }
  this.laps_ = race.raceSession.laps;
  this.maxLapTime_ = race.raceSession.maxLapTimeMs;
  this.quickRace_ = race.raceSession.quickRace;
};


/**
 * @param {object} msg
 * @param {number} timestamp
 */
Race.prototype.parseOutput = function(msg, timestamp) {
  // TODO: Display what we send to the server.
};
});
