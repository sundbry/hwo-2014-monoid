
/** @constructor */
function RaceMessage(){};

/** @type {TrackMessage} */
RaceMessage.prototype.track;
/** @type !Array.<CarMessage> */
RaceMessage.prototype.cars;
/** @type {{laps: number, maxLapTimeMs: number, quickRace: boolean}} */
RaceMessage.prototype.raceSession;

/** @constructor */
function CarMessage(){};

/** @constructor */
function TrackMessage(){};

/** @constructor */
function GenericMessage(){};
/** @type {?} */
GenericMessage.prototype.data;
/** @type {string} msgType */
GenericMessage.prototype.msgType;
/** @type {number} */
GenericMessage.prototype.gameTick;
