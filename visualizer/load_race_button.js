goog.provide('monoid.LoadRaceButton');

goog.require('goog.ui.Component');
goog.require('goog.dom.classlist');

goog.scope(function(){



/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.LoadRaceButton = function() {
  goog.base(this);
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
};

});