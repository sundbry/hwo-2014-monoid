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

  /** @private */
  this.totalTicks_ = 0;

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
	var lineLimit = lines.length; // Math.min(lines.length, 100); // for testing
  for (var i = 0; i < lineLimit; i++) {
    if (!lines[i] ||
				(lines[i].substring(0, 32).indexOf('track') >= 0)) // Skip long "track" messages
			continue;

    var line = eval(lines[i]);
		var data = line[2];
		switch (line[1]) {
			case 'in':
      	this.parseInput(data, line[0]);
				break;
			case 'out':
	      this.parseOutput(data, line[0]);
				break;
			case 'dashboard':
				// this.parseDashboard(data);
			default:
				break;
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

Race.prototype.parseDashboard = function(msg) {
	// update dashboard ui
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
  // We get a position without a game tick before the game starts
  // as well as after it ends. Set tick 0 position for the first case.
  if (!gameTick && this.totalTicks_ == 0) gameTick = 0;
  if (gameTick === undefined) return;

  this.totalTicks_ = Math.max(this.totalTicks_, gameTick);
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
 * @returns {number}
 */
Race.prototype.getTotalGameTicks = function() {
  return this.totalTicks_;
};


/**
 * @returns {monoid.Track}
 */
Race.prototype.getTrack = function() {
  return this.track_;
};


/**
 * @returns {!Array.<!monoid.Car>}
 */
Race.prototype.getCars = function() {
  return this.cars_;
};


/**
 * @param {Object} msg
 * @param {number} timestamp
 */
Race.prototype.parseOutput = function(msg, timestamp) {
  // TODO: Display what we send to the server.
};
});
