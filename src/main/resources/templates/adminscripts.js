function reIndex() {
    postRequestToServer("reIndex", {}, function(response) {
                                            document.getElementById("adminInfo").value = response.status;
                                        }
                       );
}

function reInitialize() {
    payload = JSON.stringify({"input":document.getElementById("file").value});
    postRequestToServer("reInitialize", payload, function(response) {
                                            document.getElementById("adminInfo").value = response.status;
                                        }
                       );
}

function dumpQuestions() {
    postRequestToServer("dumpQuestions", {}, function(response) {
                                            json = JSON.stringify(response.debugInfo,null,2);
                                            x = json.replace(/\"/g, "");
                                            document.getElementById("adminInfo").value = x;
                                        }
                       );
}
function unansweredQuestions() {
    postRequestToServer("unansweredQuestions", {}, function(response) {
                                            json = JSON.stringify(response.debugInfo,null,2);
                                            x = json.replace(/\"/g, "");
                                            document.getElementById("adminInfo").value = x;
                                        }
                       );
}
function parameters() {
    postRequestToServer("parameters", {}, function(response) {
                                            json = JSON.stringify(response.debugInfo,null,2);
                                            x = json.replace(/\"/g, "");
                                            document.getElementById("adminInfo").value = x;
                                        }
                       );
}