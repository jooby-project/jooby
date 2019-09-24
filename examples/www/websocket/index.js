var webSocket;
var output = document.getElementById("output");
var connectBtn = document.getElementById("connectBtn");
var sendBtn = document.getElementById("sendBtn");
var closeBtn = document.getElementById("closeBtn");
var protocol = window.location.pathname === "/secure" ? "wss" : "ws";
var port = protocol === "wss" ? 8043 : 8080;
function connect() {
  // open the connection if one does not exist
  if (webSocket !== undefined
      && webSocket.readyState !== WebSocket.CLOSED) {
    return;
  }

  // Create a websocket
  webSocket = new WebSocket(protocol + "://localhost:" + port + "/ws");

  webSocket.onopen = function (event) {
    updateOutput("Connected!");
    connectBtn.disabled = true;
    sendBtn.disabled = false;
    closeBtn.disabled = false;
  };

  webSocket.onmessage = function (event) {
    updateOutput(event.data);
  };

  webSocket.onclose = function (event) {
    updateOutput("Connection Closed");
    connectBtn.disabled = false;
    sendBtn.disabled = true;
  };
}

function send() {
  var text = document.getElementById("input").value;
  webSocket.send(text);
}

function closeSocket() {
  webSocket.close();
}

function updateOutput(text) {
  output.innerHTML += "<br/>" + text;
}
