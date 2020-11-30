package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.*;
import org.arjunkashyap.learningbot.common.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class FeedbackComponentController {
    @Autowired private Utilities<Map<String, Object>> utilities;

    @PostMapping(
            value = "/receiveFeedback", consumes = "application/json", produces = "application/json")
    public BotResponse receiveFeedback(@RequestBody BotRequest request) {//TODO: Register feedback, send acknowledgement
        //TODO: Log each interaction and the response in table
        BotResponse botResponse = new BotResponse();
        return botResponse;
    }

    @PostMapping(
            value = "/getNextAnswer", consumes = "application/json", produces = "application/json")
    public AnswerResponse getNextAnswer(@RequestBody BotRequest request) {
        long startTime = System.currentTimeMillis();
        System.out.println(request.getContext());
        Map<String, Object> context = utilities.deserializeFromString(request.getContext());
        //System.out.println("Last Index: "+lastAnswerIndex);
        //System.out.println("Previous Response: "+matches.get(lastAnswerIndex).getAnswer().getAnswerString());
        AnswerResponse answerResponse = new AnswerResponse();
        int lastAnswerIndex = Integer.MAX_VALUE;
        Question question = null;
        List<Match> matches = null;
        if (context != null && context.get("LAST_ANSWER_INDEX") != null){
            lastAnswerIndex = (int) context.get("LAST_ANSWER_INDEX");
            question = (Question) context.get("QUESTION");
            matches = (List<Match>) context.get("LAST_MATCHES");
        }
        if (matches == null || lastAnswerIndex >= matches.size()-1) {//Ideally this should be handled in UI by not enabling the button
            //TODO
            answerResponse.setMatches(matches);
            Answer answer = new Answer();
            answer.setAnswerId(-1);
            answer.setAnswerString("You should not have been able to get this");
            answerResponse.setTopAnswer(answer);
            answerResponse.setContext(null);
            answerResponse.setStatus("FAILURE");
        }
        else {
            answerResponse.setMatches(matches);
            answerResponse.setTopAnswer(matches.get(lastAnswerIndex + 1).getAnswer());
            context = new HashMap<>();
            context.put("QUESTION", question);
            context.put("LAST_ANSWER_INDEX", lastAnswerIndex + 1);
            context.put("LAST_MATCHES", matches);
            answerResponse.setContext(utilities.serializeToString(context));
            answerResponse.setStatus("SUCCESS");
            answerResponse.setDebugInfo(question.toString() + matches.toString()); //TODO: Make debugInfo Json?
            //TODO: reduce vote
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        answerResponse.setResponseTime(elapsedTime);
        //System.out.println(String.format("ResponseTime of [%s]: [%d]", AnsweringComponentController.class, elapsedTime));//TODO: log in table
        //TODO: Log each interaction and the response in table
        return answerResponse;
    }

    //TODO: Method for not detecting a question properly

    //TODO: Method for marking incorrect answer. What if a question is marked with too many down-votes.
    // We may be giving some other answer that is not appropriate

}
