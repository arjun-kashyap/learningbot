package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.*;
import org.arjunkashyap.learningbot.common.Utilities;
import org.arjunkashyap.learningbot.process.FeedbackProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
public class FeedbackComponentController {
    @Autowired private Utilities<Map<String, Object>> utilities;
    @Autowired private FeedbackProcessor feedbackProcessor;
    @Autowired private JdbcTemplate jtm;

    @PostMapping(
            value = "/questionDetectionError", consumes = "application/json", produces = "application/json")
    public BotResponse questionDetectionError(@RequestBody BotRequest request) {//TODO: Register feedback, send acknowledgement
        //TODO: Log each interaction and the response in table
        BotResponse botResponse = new BotResponse();
        return botResponse;
    }

    @PostMapping(
            value = "/getNextAnswer", consumes = "application/json", produces = "application/json")
    public AnswerResponse getNextAnswer(@RequestBody BotRequest request) {
        long startTime = System.currentTimeMillis();
        AnswerResponse answerResponse = new AnswerResponse();
        Map<String, Object> context = null;
        if (request.getContext() != null) {
            context = utilities.deserializeFromString(request.getContext());
        }
        //System.out.println("Last Index: "+lastAnswerIndex);
        //System.out.println("Previous Response: "+matches.get(lastAnswerIndex).getAnswer().getAnswerString());
        int lastAnswerIndex = Integer.MAX_VALUE;
        Question inputQuestion = null;
        List<Match> matches = null;
        if (context != null && context.get("LAST_ANSWER_INDEX") != null){
            lastAnswerIndex = (int) context.get("LAST_ANSWER_INDEX");
            inputQuestion = (Question) context.get("QUESTION");
            matches = (List<Match>) context.get("LAST_MATCHES");
        }
        if (matches == null || lastAnswerIndex >= matches.size()-1) {//Ideally this should be handled in UI by not enabling the button
            //TODO
            matches = new ArrayList<>();
            Match match = new Match();
            Answer answer = new Answer();
            answer.setAnswerId(-1);
            answer.setAnswerString("You should not have been able to get this");
            match.setAnswer(answer);
            matches.add(match);
            answerResponse.setTopMatchIndex(0);
            answerResponse.setMatches(matches);
            answerResponse.setContext(null);
            answerResponse.setStatus("FAILURE");
        }
        else {
            answerResponse.setMatches(matches);
            answerResponse.setTopMatchIndex(lastAnswerIndex + 1);
            context = new HashMap<>();
            context.put("QUESTION", inputQuestion);
            context.put("LAST_ANSWER_INDEX", lastAnswerIndex + 1);
            context.put("LAST_MATCHES", matches);
            answerResponse.setContext(utilities.serializeToString(context));
            answerResponse.setStatus("SUCCESS");
            //answerResponse.setDebugInfo(question.toString() + matches.toString()); //TODO: Make debugInfo Json?
            Map<String, Object> debugInfo = new TreeMap<>();
            debugInfo.put("AskedQuestion", inputQuestion);
            debugInfo.put("CurrentMatch", lastAnswerIndex + 1);
            debugInfo.put("Matches", matches);
            answerResponse.setDebugInfo(debugInfo);
            //TODO: reduce vote
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        answerResponse.setResponseTime(elapsedTime);
        //System.out.println(String.format("ResponseTime of [%s]: [%d]", AnsweringComponentController.class, elapsedTime));//TODO: log in table
        //TODO: Log each interaction and the response in table
        jtm.update("INSERT INTO INTERACTION (interaction_id, interaction_type, response_time_millis, create_date) values (INTERACTION_SEQUENCE.NEXTVAL, 'NEXT_ANSWER', ?, CURRENT_TIMESTAMP())", elapsedTime);
        return answerResponse;
    }

    @PostMapping(
            value = "/markAsCorrect", consumes = "application/json", produces = "application/json")
    public BotResponse markAsCorrect(@RequestBody BotRequest request) {
        long startTime = System.currentTimeMillis();
        BotResponse botResponse = new BotResponse();
        Map<String, Object> context = null;
        context = utilities.deserializeFromString(request.getContext());
        if (context != null && context.size()>0) {
            int lastAnswerIndex = (int) context.get("LAST_ANSWER_INDEX");
            Question question = (Question) context.get("QUESTION");
            List<Match> matches = (List<Match>) context.get("LAST_MATCHES");
            Match lastMatch = matches.get(lastAnswerIndex);
            System.out.println(lastMatch);
            feedbackProcessor.incrementVote(lastMatch, question);
            Map<String, Object> message = new HashMap<>();
            message.put("message","Feedback processed");
            botResponse.setDebugInfo(message);
            botResponse.setStatus("SUCCESS");
        } else {
            Map<String, Object> message = new HashMap<>();
            message.put("message","Handle in UI");
            botResponse.setDebugInfo(message);
            botResponse.setStatus("FAILURE");
        }
        botResponse.setContext(request.getContext());
        long elapsedTime = System.currentTimeMillis() - startTime;
        jtm.update("INSERT INTO INTERACTION (interaction_id, interaction_type, response_time_millis, create_date) values (INTERACTION_SEQUENCE.NEXTVAL, 'CORRECT_ANSWER', ?, CURRENT_TIMESTAMP())", elapsedTime);
        botResponse.setResponseTime(elapsedTime);
        return botResponse;
    }

    @PostMapping(
            value = "/markAsIncorrect", consumes = "application/json", produces = "application/json")
    public BotResponse markAsIncorrect(@RequestBody BotRequest request) {
        long startTime = System.currentTimeMillis();
        BotResponse botResponse = new BotResponse();
        Map<String, Object> context = null;
        context = utilities.deserializeFromString(request.getContext());
        if (context != null && context.size()>0) {

            int lastAnswerIndex = (int) context.get("LAST_ANSWER_INDEX");
            Question question = (Question) context.get("QUESTION");
            List<Match> matches = (List<Match>) context.get("LAST_MATCHES");
            Match lastMatch = matches.get(lastAnswerIndex);
            System.out.println(lastMatch);
            feedbackProcessor.decrementVote(lastMatch, question);
            Map<String, Object> message = new HashMap<>();
            message.put("message","Feedback processed");
            botResponse.setDebugInfo(message);
            botResponse.setStatus("SUCCESS");
        } else {
            Map<String, Object> message = new HashMap<>();
            message.put("message","Handle in UI");
            botResponse.setDebugInfo(message);
            botResponse.setStatus("FAILURE");
        }
        botResponse.setContext(request.getContext());

        long elapsedTime = System.currentTimeMillis() - startTime;
        jtm.update("INSERT INTO INTERACTION (interaction_id, interaction_type, response_time_millis, create_date) values (INTERACTION_SEQUENCE.NEXTVAL, 'INCORRECT_ANSWER', ?, CURRENT_TIMESTAMP())", elapsedTime);
        botResponse.setResponseTime(elapsedTime);
        return botResponse;
    }

    //TODO: Method for not detecting a question properly

    //TODO: Method for marking incorrect answer. What if a question is marked with too many down-votes.
    // We may be giving some other answer that is not appropriate

}
