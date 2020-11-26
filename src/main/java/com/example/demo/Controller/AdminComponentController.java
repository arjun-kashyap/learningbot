package com.example.demo.Controller;

import com.example.demo.Entity.AdminQuestionAnswerRelation;
import com.example.demo.Entity.BotResponse;
import com.example.demo.process.AdminProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminComponentController {
    @Autowired
    private AdminProcessor adminProcessor;

    @PostMapping(
            value = "/reIndex", consumes = "application/json", produces = "application/json")
    public BotResponse reIndex() {
        long startTime = System.currentTimeMillis();

        adminProcessor.reIndex();
        BotResponse response = new BotResponse();
        response.setStatus("Re-indexed successfully");
        long elapsedTime = System.currentTimeMillis() - startTime;
        response.setResponseTime(elapsedTime);
        return response;
    }

    @PostMapping(
            value = "/dumpQuestions", consumes = "application/json", produces = "application/json")
    public BotResponse dumpQuestions() {
        BotResponse response = new BotResponse();
        List<AdminQuestionAnswerRelation> listOfQuestions = adminProcessor.dumpQuestions();
        StringBuilder s = new StringBuilder("question_id\t\tquestion\t\t\t\t\tmax_possible_searcher_score\tanswer_id\tanswer\tmanual\tvotes");
        for (AdminQuestionAnswerRelation r: listOfQuestions) {
            s.append("\n"+r.getQuestion().getQuestionId()+
                    "\t"+r.getQuestion().getQuestionString()+
                    "\t"+r.getQuestion().getMaxPossibleSearcherScore()+
                    "\t"+r.getAnswer().getAnswerId()+
                    "\t"+r.getAnswer().getAnswerString()+
                    "\t"+r.isManual()+
                    "\t"+r.getVotes()
            );
        }
        response.setDebugInfo(s.toString());//TODO: spacing of header and questions
        return response;
    }

    //TODO: Write code to show satisfaction rate, adjust scoring parameters, etc

    //TODO: Method to adjust parameters

    //TODO: Method to show parameter values
}