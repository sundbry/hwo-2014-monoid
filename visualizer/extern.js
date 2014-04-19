
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
/** @type {string} */
TrackMessage.prototype.id;
/** @type {string} */
TrackMessage.prototype.name;
/** @type {Array.<{length: number, switch: boolean, radius: number, angle: number}>} */
TrackMessage.prototype.pieces;
/** @type {Array.<{distanceFromCenter: number, index: number}>} */
TrackMessage.prototype.lanes;
/** @type {{position: {x: number, y: number}, angle: number}} */
TrackMessage.prototype.startingPoint;

/** @constructor */
function GenericMessage(){};
/** @type {?} */
GenericMessage.prototype.data;
/** @type {string} msgType */
GenericMessage.prototype.msgType;
/** @type {number} */
GenericMessage.prototype.gameTick;
