package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.AnswerResponse;
import org.arjunkashyap.learningbot.Entity.BotRequest;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.common.Utilities;
import org.arjunkashyap.learningbot.process.AnswerProcessor;
import org.arjunkashyap.learningbot.process.KnowledgeProcessor;
import org.arjunkashyap.learningbot.common.SentenceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AnsweringComponentController {
    @Autowired private AnswerProcessor answerProcessor;
    @Autowired private SentenceAnalyzer sentenceAnalyzer;
    @Autowired private KnowledgeProcessor knowledgeProcessor;
    @Autowired private Utilities<Map<String, Object>> utilities;

    @PostMapping(
            value = "/postQuestion", consumes = "application/json", produces = "application/json")
    public AnswerResponse postQuestion(@RequestBody BotRequest request) {
        long startTime = System.currentTimeMillis();
        Question question = sentenceAnalyzer.processQuestion(request.getInput());
        List<Match> matches = answerProcessor.getAnswer(question);

        AnswerResponse answerResponse = new AnswerResponse();
        answerResponse.setMatches(matches);
        answerResponse.setTopAnswer(matches.get(0).getAnswer());
        //answerResponse.setTopAnswer(matches.first().getAnswer());
        Map<String, Object> context = new HashMap<>();
        context.put("QUESTION", question);
        context.put("LAST_ANSWER_INDEX", 0);
        context.put("LAST_MATCHES", matches);
        answerResponse.setContext(utilities.serializeToString(context));
        long elapsedTime = System.currentTimeMillis() - startTime;
        answerResponse.setResponseTime(elapsedTime);
        //System.out.println(String.format("ResponseTime of [%s]: [%d]", AnsweringComponentController.class, elapsedTime));//TODO: log in table
        //TODO: Log each interaction and the response in table
        //TODO: If the question text had different nouns and verbs, and the vote count increases add the question to the database
        //      The question table can have a new column to indicate if the question was automatically added
        //
        //TODO: Use NER parser of CoreNLP to detect names of people and organization and use in addition to nouns and verbs
        // See https://www.aclweb.org/anthology/P14-5010.pdf

        answerResponse.setStatus("SUCCESS");
        answerResponse.setDebugInfo(question.toString()+matches.toString()); //TODO: Make debugInfo Json?
        return answerResponse;
    }
}
