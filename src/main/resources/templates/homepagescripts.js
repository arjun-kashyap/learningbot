var context;
function getAnswer() {
    var question = document.getElementById("question").value.trim();
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    conversation = conversation + "\nYou: " + question;
    conversationTextarea.value = conversation;
    document.getElementById("question").value = "";
    document.getElementById("submit").value = "Please wait";
    document.getElementById("submit").disabled = true;
    payload = JSON.stringify({"input":question});
    postRequestToServer("postQuestion", payload, function(response) {
                                            topMatch = response.matches[response.topMatchIndex];
                                            conversationTextarea.value = conversation + "\nChatBot: " + topMatch.answer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            document.getElementById("responseTime").value = response.responseTime;
                                            document.getElementById("confidence").value = topMatch.weightedFinalScore;
                                            document.getElementById("confidencePercent").value = Math.round(topMatch.weightedFinalScore*100,0)+'%';
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            document.getElementById("submit").value = "Submit";
                                            document.getElementById("submit").disabled = false;
                                            if (response.matches.length-1 == response.topMatchIndex) {
                                                document.getElementById("next_answer").disabled = true;
                                            } else {
                                                document.getElementById("next_answer").disabled = false;
                                            }
                                            context = response.context;
                                    }
                          );
}

function getNextAnswer() {
    payload = JSON.stringify({"context":context});
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    document.getElementById("next_answer").value = "Please wait";
    document.getElementById("next_answer").disabled = true;
    postRequestToServer("getNextAnswer", payload, function(response) {
                                            topMatch = response.matches[response.topMatchIndex];
                                            conversationTextarea.value = conversation + "ChatBot: " + topMatch.answer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            document.getElementById("responseTime").value = response.responseTime;
                                            document.getElementById("confidence").value = topMatch.weightedFinalScore;
                                            document.getElementById("confidencePercent").value = Math.round(topMatch.weightedFinalScore*100,0)+'%';
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            document.getElementById("next_answer").value = "Get another answer";
                                            document.getElementById("next_answer").disabled = false;
                                            if (response.matches.length-1 == response.topMatchIndex) {
                                                document.getElementById("next_answer").disabled = true;
                                            } else {
                                                document.getElementById("next_answer").disabled = false;
                                            }
                                            context = response.context;
                                                                     }
                          );
}

function markAsCorrect() {
    payload = JSON.stringify({"context":context});
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    postRequestToServer("markAsCorrect", payload, function(response) {
                                            conversationTextarea.value = conversation + "ChatBot: " + response.topAnswer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            context = response.context;
                                                                     }
                          );
}

function markAsIncorrect() {
    payload = JSON.stringify({"context":context});
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    postRequestToServer("markAsIncorrect", payload, function(response) {
                                            conversationTextarea.value = conversation + "ChatBot: " + response.topAnswer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            context = response.context;
                                                                     }
                          );
}