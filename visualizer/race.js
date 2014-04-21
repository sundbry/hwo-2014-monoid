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

  /** @private {!Array.<!monoid.Car>} */
  this.cars_ = [];

  this.parseLog_(raceLog.split('\n'));
};
var Race = monoid.Race;

/** @private goog.log.Logger */
Race.logger_ = goog.log.getLogger('monoid.Race');

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
 * @param {GenericMessage} msg
 * @param {number} timestamp
 */
Race.prototype.parseInput = function(msg, timestamp) {
  switch (msg.msgType) {
   case 'gameInit':
    this.gameInit(msg.data.race, timestamp);
    break;
   case 'carPositions':
    this.setPositionsAt(msg.data, msg.gameTick);
    break;
   default:
    goog.log.info(Race.logger_, 'Unknown message type: ' + msg.msgType);
  }
};


/**
 * @param {RaceMessage} race
 * @param {number} timestamp
 */
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
 * @param {!Array.<CarPositionMessage>} positions
 * @param {number} gameTick
 */
Race.prototype.setPositionsAt = function(positions, gameTick) {
  for (var i = 0; i < positions.length; i++) {
    var id = new monoid.Car.Id(positions[i].id);
    var car = this.findCar(id);
    if (car) {
      car.setPositionAt(positions[i], gameTick, this.track_.getPieces());
    } else {
      goog.log.warning(Race.logger_, 'Can\' find car with id: '+id);
    }
  }
};


/**
 * @param {monoid.Car.Id} id
 * @returns {monoid.Car}
 */
Race.prototype.findCar = function(id) {
  for (var i = 0; i < this.cars_.length; i++) {
    if (this.cars_[i].getId().equals(id)) return this.cars_[i];
  }
  return null;
};


/**
 * @returns {monoid.Track}
 */
Race.prototype.getTrack = function() {
  return this.track_;
};


/**
 * @param {Object} msg
 * @param {number} timestamp
 */
Race.prototype.parseOutput = function(msg, timestamp) {
  // TODO: Display what we send to the server.
};
});
