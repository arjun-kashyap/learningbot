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


function getNextAnswer() {
    payload = JSON.stringify({"context":context});
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    postRequestToServer("getNextAnswer", payload, function(response) {
                                            conversationTextarea.value = conversation + "ChatBot: " + response.topAnswer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            context = response.context;
                                                                     }
                          );
}