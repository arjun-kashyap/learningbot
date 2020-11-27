package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.BotResponse;
import org.arjunkashyap.learningbot.Entity.Question;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedbackComponentController {
    @PostMapping(
            value = "/receiveFeedback", consumes = "application/json", produces = "application/json")
    public BotResponse receiveFeedback(@RequestBody Question question) {//TODO: Register feedback, send acknowledgement
        //TODO: Log each interaction and the response in table
        BotResponse botResponse = new BotResponse();
        return botResponse;
    }
    //TODO: Method for sending back another answer

    //TODO: Method for not detecting a question properly

    //TODO: Method for marking incorrect answer. What if a question is marked with too many down-votes.
    // We may be giving some other answer that is not appropriate

}
