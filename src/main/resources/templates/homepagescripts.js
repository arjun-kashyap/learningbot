var context;

function getAnswer() {
    var question = document.getElementById("question").value.trim();
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    conversation = conversation + "\nYou: " + question;
    conversationTextarea.value = conversation;
    document.getElementById("question").value = "";
    payload = JSON.stringify({"input":question});
    postRequestToServer("postQuestion", payload, function(response) {
                                            topMatch = response.matches[response.topMatchIndex];
                                            conversationTextarea.value = conversation + "\nChatBot: " + topMatch.answer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            document.getElementById("responseTime").value = response.responseTime;
                                            document.getElementById("confidence").value = topMatch.weightedFinalScore;
                                            document.getElementById("confidencePercent").value = Math.round(topMatch.weightedFinalScore*100,0)+'%';
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            if (topMatch.question == null) {
                                                disableFeedback();
                                            } else {
                                                enableFeedback();
                                            }

                                            if (response.matches.length-1 == response.topMatchIndex) {
                                                document.getElementById("next_answer").disabled = true;
                                            } else {
                                                document.getElementById("next_answer").disabled = false;
                                            }
                                            if (topMatch.answer.answerId == -1) {
                                                document.getElementById("question_error").disabled = false;
                                            } else {
                                                document.getElementById("question_error").disabled = true;
                                            }

                                            context = response.context;
                                    }
                          );
}

function getNextAnswer() {
    payload = JSON.stringify({"context":context});
    var conversationTextarea = document.getElementById("conversation");
    var conversation = conversationTextarea.value;
    postRequestToServer("getNextAnswer", payload, function(response) {
                                            topMatch = response.matches[response.topMatchIndex];
                                            conversationTextarea.value = conversation + "ChatBot: " + topMatch.answer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            document.getElementById("responseTime").value = response.responseTime;
                                            document.getElementById("confidence").value = topMatch.weightedFinalScore;
                                            document.getElementById("confidencePercent").value = Math.round(topMatch.weightedFinalScore*100,0)+'%';
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            enableFeedback();
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
    disableFeedback();
    postRequestToServer("markAsCorrect", payload, function(response) {
                                            context = response.context;
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            alert("Thanks for your feedback");
                                                                     }
                          );
}

function markAsIncorrect() {
    payload = JSON.stringify({"context":context});
    state = document.getElementById("next_answer").disabled;
    disableFeedback();
    document.getElementById("next_answer").disabled = state;
    postRequestToServer("markAsIncorrect", payload, function(response) {
                                            context = response.context;
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            alert("Thanks for your feedback");
                                                                     }
                          );
}
function questionDetectionError() {
    payload = JSON.stringify({"context":context});
    disableFeedback();
    postRequestToServer("questionDetectionError", payload, function(response) {
                                            context = response.context;
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            alert("Thanks for your feedback");
                                                                     }
                          );
}

function disableFeedback() {
    document.getElementById("question_error").disabled = true;
    document.getElementById("correct_answer").disabled = true;
    document.getElementById("incorrect_answer").disabled = true;
    document.getElementById("next_answer").disabled = true;
}

function enableFeedback() {
    document.getElementById("question_error").disabled = false;
    document.getElementById("correct_answer").disabled = false;
    document.getElementById("incorrect_answer").disabled = false;
    document.getElementById("next_answer").disabled = false;
}