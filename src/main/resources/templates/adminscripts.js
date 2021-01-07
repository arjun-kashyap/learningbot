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
                                            json = JSON.stringify(response.debugInfo,null,2);
                                            x = json.replace(/\"/g, "");
                                            document.getElementById("adminInfo").value = x;
                                        }
                       );
}