package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.AdminQuestionAnswerRelation;
import org.arjunkashyap.learningbot.Entity.BotResponse;
import org.arjunkashyap.learningbot.process.AdminProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class AdminComponentController {
    @Autowired private AdminProcessor adminProcessor;
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
            value = "/reInitialize", consumes = "application/json", produces = "application/json")
    public BotResponse reInitialize() {
        long startTime = System.currentTimeMillis();

        adminProcessor.reInitialize();
        BotResponse response = new BotResponse();
        response.setStatus("Reinitialized successfully");
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
        Map<String, Object> debugInfo = new TreeMap<>();
        debugInfo.put("#", s.toString());
        int i = 0;
        for (AdminQuestionAnswerRelation r: listOfQuestions) {
            i++;
            debugInfo.put(i+"", "\n"+r.getQuestion().getQuestionId()+
                    "\t"+r.getQuestion().getQuestionString()+
                    "\t"+r.getQuestion().getMaxPossibleScoreForMainWords()+
                    "\t"+r.getQuestion().getMaxPossibleScoreForSynsets()+
                    "\t"+r.getAnswer().getAnswerId()+
                    "\t"+r.getAnswer().getAnswerString()+
                    "\t"+r.isManual()+
                    "\t"+r.getVotes()
            );
        }
        //response.setDebugInfo(s.toString());//TODO: LOW: spacing of header and questions
        response.setDebugInfo(debugInfo);

        return response;
    }

    //TODO: Write code to show satisfaction rate, adjust scoring parameters, etc

    //TODO: Method to adjust parameters

    //TODO: Method to show parameter values
}