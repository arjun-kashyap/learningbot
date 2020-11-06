package com.example.demo.Controller;

import com.example.demo.process.QuestionIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminComponentController {
    @Autowired
    private QuestionIndexer questionIndexer;

    @PostMapping(
            value = "/reIndex", consumes = "application/json", produces = "application/json")
    public void reIndex() {
        questionIndexer.init();
    }

}
