package com.example.demo.Controller;

import com.example.demo.Entity.BotResponse;
import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FeedbackComponentController {
    @PostMapping(
            value = "/receiveFeedback", consumes = "application/json", produces = "application/json")
    public BotResponse receiveFeedback(@RequestBody Question question) {//TODO: Register feedback, send acknowledgement
        BotResponse botResponse = new BotResponse();
        return botResponse;
    }

}
