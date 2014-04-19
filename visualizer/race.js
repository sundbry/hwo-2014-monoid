goog.provide('monoid.Race');

goog.scope(function() {



/**
 * @param {string} raceLog log of a single race
 * @constructor
 */
monoid.Race = function(raceLog) {
  this.parseLog_(raceLog.split('\n'));
};
var Race = monoid.Race;



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

};


/**
 * @param {object} msg
 * @param {number} timestamp
 */
Race.prototype.parseOutput = function(msg, timestamp) {
  // TODO: Display what we send to the server.
};
});
