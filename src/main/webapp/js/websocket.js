/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//var pathname ="/protein-tertiary-structure-web-service/";
var pathname = "/smc_genome/";
//alert('here');
//var wsUri = "ws://" + document.location.host + document.location.pathname + "whiteboardendpoint";
var wsUri = "ws://" + document.location.host + pathname + "endpoint_smc_genome";
var websocket = new WebSocket(wsUri);
var date1, date2;
websocket.onerror = function (evt) {
    onError(evt)
};

function onError(evt) {
    writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data);
}
var output = document.getElementById("output");
websocket.onopen = function (evt) {
    onOpen(evt)
};

function writeToScreen(message) {
    output.innerHTML += message + "<br>";
}

function onOpen() {
    writeToScreen("Connected to Server for secure computation");
}
websocket.onmessage = function (event) {
    console.log(JSON.stringify(event.data));
    var msg = JSON.parse(event.data);
    writeToScreen("received \"" + msg.msg + "\"");
    if (msg.hasOwnProperty('type')) {
        if (msg.type != "info") {
            date2 = new Date();
            var timeDiff = Math.abs(date2.getTime() - date1.getTime());

//    writeToScreen("Ended on "+date2);
            writeToScreen("Time Difference <h3>" + timeDiff / (1000) + "</h3>");
        }
    }


}
websocket.onclose = function () {
    writeToScreen('<p class="event">Socket Status: ' + websocket.readyState + ' (Closed)');
}
$(document).ready(function () {
    $("#nCount").hide();
    $('#operation').on('change', function () {
//        console.log(this.value);
        if (this.value === "editdist_OPE") {
            $("#is_secure").hide();
            $("#is_secure").prop('checked', false);
        }
        if (this.value === "editdist") {
            $("#nCount").show();

        } else {
            $("#nCount").hide();
            $("#is_secure").prop('checked', true);
        }

    });
    $("#send").on("click", function (event) {
        output.innerHTML = "";
        date1 = new Date();
        writeToScreen("<p>Sent: {type:'q',msg:{operation:'" + $("#operation").val() + "',count:'" + $("#count").val() + "',secure:'" + ($("#is_secure").is(':checked') ? 1 : 0) + "',text:'" + $("#text").val() + "',snip:'" + $("#snipID").val() + "',type:'" + $("#type").val() + "'}}</p>");
        writeToScreen("Started on " + date1);
        websocket.send(JSON.stringify(eval("({type:'q',msg:{operation:'" + $("#operation").val() + "',count:'" + $("#count").val() + "',secure:'" + ($("#is_secure").is(':checked') ? 1 : 0) + "',text:'" + $("#text").val() + "',snip:'" + $("#snipID").val() + "',type:'" + $("#type").val() + "'}})")));
    });

});

