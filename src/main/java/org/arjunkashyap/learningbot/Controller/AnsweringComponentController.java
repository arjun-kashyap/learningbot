package org.arjunkashyap.learningbot.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.sf.extjwnl.JWNLException;
import org.arjunkashyap.learningbot.Entity.AnswerResponse;
import org.arjunkashyap.learningbot.Entity.BotRequest;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.common.Utilities;
import org.arjunkashyap.learningbot.process.AnswerProcessor;
import org.arjunkashyap.learningbot.process.GeneralProcessor;
import org.arjunkashyap.learningbot.process.KnowledgeProcessor;
import org.arjunkashyap.learningbot.common.SentenceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AnsweringComponentController {
    @Autowired private AnswerProcessor answerProcessor;
    @Autowired private SentenceAnalyzer sentenceAnalyzer;
    @Autowired private KnowledgeProcessor knowledgeProcessor;
    @Autowired private Utilities<Map<String, Object>> utilities;
    @Autowired private JdbcTemplate jtm;
    @Autowired private GeneralProcessor generalProcessor;

    @PostMapping(
            value = "/postQuestion", consumes = "application/json", produces = "application/json")
    public AnswerResponse postQuestion(@RequestBody BotRequest request) throws JWNLException, JsonProcessingException {
        long startTime = System.currentTimeMillis();
        Question inputQuestion = sentenceAnalyzer.processQuestion(request.getInput());
        List<Match> matches = answerProcessor.getAnswer(inputQuestion);

        AnswerResponse answerResponse = new AnswerResponse();
        answerResponse.setMatches(matches);
        answerResponse.setTopMatchIndex(0);
        Map<String, Object> context = new HashMap<>();
        context.put("QUESTION", inputQuestion);
        context.put("LAST_ANSWER_INDEX", 0);
        context.put("LAST_MATCHES", matches);
        answerResponse.setContext(utilities.serializeToString(context));
        long elapsedTime = System.currentTimeMillis() - startTime;
        answerResponse.setResponseTime(elapsedTime);
        //System.out.println(String.format("ResponseTime of [%s]: [%d]", AnsweringComponentController.class, elapsedTime));
        //
        //Used NER parser of CoreNLP to detect names of people and organization and use in addition to nouns and verbs
        // See https://www.aclweb.org/anthology/P14-5010.pdf

        generalProcessor.logInteraction("ANSWER", elapsedTime, 1);
        answerResponse.setStatus("SUCCESS");
        Map<String, Object> debugInfo = new TreeMap<>();
        debugInfo.put("AskedQuestion", inputQuestion);
        debugInfo.put("CurrentMatch", 0);
        debugInfo.put("Matches", matches);
        answerResponse.setDebugInfo(debugInfo);
        return answerResponse;
    }
}
