SignalPath.Input = function(json, parentDiv, module, type, pub) {
	pub = pub || {};
	pub = SignalPath.Endpoint(json, parentDiv, module, type || "input", pub);
	
	var super_createDiv = pub.createDiv;
	pub.createDiv = function() {
		var div = super_createDiv();

		div.bind("spConnect", (function(me) {
			return function(event, output) {
				me.json.connected = true;
				me.json.sourceId = output.getId();
				me.div.addClass("connected");
			}
		})(pub));
		
		div.bind("spDisconnect", (function(me) {
			return function(event, output) {
				me.json.connected = false;
				delete me.json.sourceId;
				me.div.removeClass("connected");
				
				if (!(pub.getInitialValue()===null || pub.getInitialValue()===undefined)) {
					me.removeClass("warning")
				}
			}
		})(pub));
		
		return div;
	}
	
	var super_createSettings = pub.createSettings;
	pub.createSettings = function(div,data) {
		super_createSettings(div,data);
		
		// Driving input. Default true.
		
		var switchDiv = $("<div class='switchContainer showOnFocus'></div>");
		div.append(switchDiv);
		
		if (data.canToggleDrivingInput==null || data.canToggleDrivingInput) {
			var driving = new SignalPath.IOSwitch(switchDiv, "ioSwitch drivingInput", {
				getValue: (function(d){
					return function() { return d.drivingInput; };
				})(data),
				setValue: (function(d){
					return function(value) { return d.drivingInput = value; };
				})(data),
				buttonText: function() { return "DR"; },
				tooltip: 'Driving input'
			});
		}
		
		// Initial value. Default null/off. Only valid for TimeSeries type
		if (data.type=="Double" && (data.canHaveInitialValue==null || data.canHaveInitialValue)) {
			var iv = new SignalPath.IOSwitch(switchDiv, "ioSwitch initialValue", {
				getValue: (function(d){
					return function() { return d.initialValue; };
				})(data),
				setValue: (function(d){
					return function(value) { return d.initialValue = value; };
				})(data),
				buttonText: function() { 
					if (this.getValue()==null)
						return "IV";
					else return this.getValue().toString();
				},
				tooltip: 'Initial value',
				nextValue: function(currentValue) {
					if (currentValue==null)
						return 0;
					else if (currentValue==0)
						return 1;
					else return null;
				},
				isActiveValue: function(currentValue) {
					return currentValue != null;
				}
			});
			
			// Remove requiresConnection class on update if input has initial value
			if (pub.json.requiresConnection) {
				$(iv).on("updated", function(e) {
					if (iv.isActiveValue(iv.getValue()))
						pub.removeClass("warning")
					else if (!pub.isConnected())
						pub.addClass("warning")
				})
				iv.update()
			}
		} 
		
		// Feedback connection. Default false. Switchable for TimeSeries types.
		
		if (data.type=="Double" && (data.canBeFeedback==null || data.canBeFeedback)) {
			var feedback = new SignalPath.IOSwitch(switchDiv, "ioSwitch feedback", {
				getValue: (function(d){
					return function() { return d.feedback; };
				})(data),
				setValue: (function(d){
					return function(value) { return d.feedback = value; };
				})(data),
				buttonText: function() { return "FB"; },
				tooltip: 'Feedback connection'
			});
		}
	}
	
	function getInitialValue() {
		return pub.json.initialValue;
	}
	pub.getInitialValue = getInitialValue;
	
	var super_getJSPlumbEndpointOptions = pub.getJSPlumbEndpointOptions;
	pub.getJSPlumbEndpointOptions = function(json,connDiv) {
		var opts = super_getJSPlumbEndpointOptions(json,connDiv);
		
		opts.connectorOverlays[0][1].direction = -1;
		opts.maxConnections = 1;
		opts.anchor = [0, 0.5, -1, 0, -15, 0];
		opts.cssClass = (opts.cssClass || "") + " jsPlumb_input";
		
		return opts;
	}
	
	pub.connect = function(endpoint) {
		jsPlumb.connect({source: pub.jsPlumbEndpoint, target:endpoint.jsPlumbEndpoint});
	}
	
	pub.getConnectedEndpoints = function() {
		var result = [];
		var connections = jsPlumb.getConnections({source:pub.getId(), scope:"*"});
		$(connections).each(function(j,connection) {
			result.push($(connection.target).data("spObject"));
		});
		return result;
	}
	
	pub.refreshConnections = function() {
		if (pub.json.sourceId!=null) {
			var connectedEndpoints = pub.getConnectedEndpoints();
			if (connectedEndpoints.length==0) {
				var endpoint = $("#"+pub.json.sourceId).data("spObject");
				if (endpoint!=null)
					pub.connect(endpoint);
				else console.log("Warning: input "+pub.getId()+" should be connected to "+pub.json.sourceId+", but the output was not found!");
			}
			else if (connectedEndpoints.length==1 && connectedEndpoints[0].getId()!=pub.json.sourceId) {
				console.log("Warning: input "+pub.getId()+" should be connected to "+pub.json.sourceId+", but is connected to "+connectedEndpoints[0].getId()+" instead!");
			}
		}
	}
	
	return pub;
}