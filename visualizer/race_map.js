goog.provide('monoid.RaceMap');

goog.require('goog.ui.Component');

goog.scope(function() {



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.RaceMap = function() {
  goog.base(this);
};
var RaceMap = monoid.RaceMap;
goog.inherits(RaceMap, goog.ui.Component);


/** @override */
RaceMap.prototype.createDom = function() {
  this.setElementInternal(this.dom_.createElement('canvas'));
}
});
