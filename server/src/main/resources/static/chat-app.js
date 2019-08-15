$(document).ready(function() {
	var MAX_HISTORY = 15;
	
	var isPWS = false; //(window.location.hostname.indexOf("cfapps.io") > -1);
	
	var ws = new WebSocket('ws://localhost:8080/websocket');
	
	// Wire up websocket so that msg received from it are shown in the 'console'.
	ws.onopen = function () {
		log('inf: WebSocket connection opened.');
		setInterval(() => {
			ws.send('Hello!');
		}, 5000);
	};
	ws.onerror = function (error) {
		log('err: '+error);
	}
	ws.onmessage = function (event) {
		log(event.data);
	};
	ws.onclose = function () {
		log('inf: WebSocket connection closed.');
	};
	
	

	function log(message) {
//		var msgs = $("#messages");
//		var newMsg = $('<li>').text(message)
//		msgs.append(newMsg);
//		newMsg.get(0).scrollIntoView();
//		while (msgs.scrollTop()>0) {
//			$("#messages").children()[0].remove();
//		}
		console.log(message);
	}

})