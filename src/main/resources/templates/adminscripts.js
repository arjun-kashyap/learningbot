function reIndex() {
    postRequestToServer("reIndex", {}, function(response) {
                                            document.getElementById("adminInfo").value = response.status;
                                        }
                       );
}

function reInitialize() {
    postRequestToServer("reInitialize", {}, function(response) {
                                            document.getElementById("adminInfo").value = response.status;
                                        }
                       );
}

function dumpQuestions() {
    postRequestToServer("dumpQuestions", {}, function(response) {
                                            document.getElementById("adminInfo").value = response.debugInfo;
                                        }
                       );
}