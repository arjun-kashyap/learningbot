package com.example.demo.Controller;

import com.example.demo.Entity.BotResponse;
import com.example.demo.process.AdminProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminComponentController {
    @Autowired
    private AdminProcessor adminProcessor;

    @PostMapping(
            value = "/reIndex", consumes = "application/json", produces = "application/json")
    public BotResponse reIndex() {
        adminProcessor.reIndex();
     return new BotResponse();
    }

    @PostMapping(
            value = "/dumpQuestions", consumes = "application/json", produces = "application/json")
    public BotResponse dumpQuestions() {
        adminProcessor.dumpQuestions();
        return new BotResponse();
    }

//TODO: Write code to show satisfaction rate, adjust scoring parameters, etc
}
