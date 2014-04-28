goog.provide('monoid.RaceChart');

goog.require('goog.ui.Component');

goog.scope(function() {

/**
 * @constructor
 * @extends {goog.ui.Component}
 */
monoid.RaceChart = function(race, valueSelectors) {
  goog.base(this);

  /** @private {Dygraph} */
  this.graph_ = null;

  this.selectors_ = valueSelectors;

  /** @type {monoid.Race} */
  this.race_ = race;
};

var RaceChart = monoid.RaceChart;
goog.inherits(RaceChart, goog.ui.Component);

/** @const {number} */
RaceChart.WIDTH = 300;

/** @const {number} */
RaceChart.HEIGHT = 220;

RaceChart.createDefaultCharts = function(race) {
  return [new RaceChart(race, ["throttle.setpoint",
                               "throttle.throttle"]),
					new RaceChart(race, ["dashboard.velocity",
                               "dashboard.acceleration"]),
          new RaceChart(race, ["dashboard.slip-angle",
                               "dashboard.slip-velocity",
                               "dashboard.slip-acceleration",
															 "dashboard.cent-acceleration"]),
          new RaceChart(race, ["dashboard.slip-angle",
                               "dashboard.turn-angle-displacement",
                               "dashboard.turn-angle-velocity"]),
					new RaceChart(race, ["calib.throttle",
															 "calib.drag",
															 "calib.k-friction",
															 "calib.acceleration-error"]),
					new RaceChart(race, ["dashboard.acceleration",
                               "calib.acceleration-estimate",
															 "calib.acceleration-error"])];
}

/** @override */
RaceChart.prototype.createDom = function() {
  this.setElementInternal(
      this.dom_.createDom('div',
                          {width: RaceChart.WIDTH, height: RaceChart.HEIGHT, "class": "chart"}));
};


/** @override */
RaceChart.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  console.log("Loading graph for: " + this.selectors_.join(" "));
  var graphOpts = {
    //customBars: true,
    // title: 'Daily Temperatures in New York vs. San Francisco',
    labels: ["Tick"].concat(this.selectors_)
    //legend: 'always',
    //labelsDivStyles: { 'textAlign': 'right' },
    //showRangeSelector: true
  };
  this.graph_ = new Dygraph(this.getElement(), this.loadData(), graphOpts);
  this.graph_.ready(function() {
    console.log("Graph loaded.");
  });
};


/** @override */
RaceChart.prototype.dispose = function() {
  if (!this.isDisposed()) {
    if (this.graph_) {
      this.graph_.destroy();
      this.graph_ = null;
    }
    goog.base(this, 'dispose');
  }
};

function matchTraceData_(trace, selector) {
  var val = null;
  var path = selector.split(".");
  var key = path.shift();

  if(trace[1] == key) {
    val = trace[2];
    while (val != null && (key = path.shift())) {
      val = val[key];
    }
    if (path.length > 0) {
      val = null;
    }
  }
  return val;
}

RaceChart.prototype.loadData = function() {
  var traceData = this.race_.getTraceData();
  var tick = 0;
  var data = [];
  var row = [0];

  for (var i = 0; i < traceData.length; i++) {
    var trace = traceData[i];
    var idx, value = null;

    var matchTick = matchTraceData_(trace, "out.gameTick")
    if (matchTick != null && row.length > 1) {
      data.push(row);
      row = [matchTick];
    }

    for (idx = 0; idx < this.selectors_.length; idx++) {
      value = matchTraceData_(trace, this.selectors_[idx]);
      if (value != null) {
        row[idx + 1] = value;
      }
    }

  }
  if (row.length > 1) {
    data.push(row);
  }

  return data;
};

});
