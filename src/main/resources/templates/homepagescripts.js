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
                                            conversationTextarea.value = conversation + "\nChatBot: " + response.topAnswer.answerString + "\n";
                                            conversationTextarea.scrollTop = conversationTextarea.scrollHeight
                                            document.getElementById("responseTime").value = response.responseTime;
                                            document.getElementById("debug").value = JSON.stringify(response.debugInfo, null, 2);
                                            document.getElementById("submit").value = "Submit";
                                            document.getElementById("submit").disabled = false;
                                            context = response.context;
                                    }
                          );
}