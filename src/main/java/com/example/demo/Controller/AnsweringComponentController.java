package com.example.demo.Controller;

import com.example.demo.Entity.AnswerResponse;
import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import com.example.demo.process.AnsweringComponentProcessor;
import com.example.demo.process.IndexProcessor;
import com.example.demo.common.SentenceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.TreeSet;

@RestController
public class AnsweringComponentController {
    @Autowired
    private AnsweringComponentProcessor answeringComponentProcessor;
    @Autowired
    private SentenceAnalyzer sentenceAnalyzer;
    @Autowired
    private IndexProcessor indexProcessor;

    @PostMapping(
            value = "/postQuestion", consumes = "application/json", produces = "application/json")
    public AnswerResponse postQuestion(@RequestBody Question question) {
        TreeSet<Match> matches;
        long startTime = System.currentTimeMillis();
        question = sentenceAnalyzer.processQuestion(question);
        matches = answeringComponentProcessor.getAnswer(question);

        AnswerResponse answerResponse = new AnswerResponse();
        answerResponse.setTopAnswer(matches.first().getAnswer());
        answerResponse.setMatchList(matches.descendingSet());
        long elapsedTime = System.currentTimeMillis() - startTime;
        answerResponse.setResponseTime(elapsedTime);
        //System.out.println(String.format("ResponseTime of [%s]: [%d]", AnsweringComponentController.class, elapsedTime));//TODO: log in table
        //TODO: Log each interaction and the response in table
        //TODO: If the question text had different nouns and verbs, and the vote count increases add the question to the database
        //      The question table can have a new column to indicate if the question was automatically added
        //
        answerResponse.setStatus("SUCCESS");
        answerResponse.setDebugInfo(new Object[]{question, matches});
        return answerResponse;
    }

    /*
    Disco http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.206.5777&rep=rep1&type=pdf
    http://www.linguatools.de/disco/disco_en.html

    wordnet similarity https://ieeexplore.ieee.org/abstract/document/7083899
    https://github.com/dmeoli/WS4J

    semilar https://www.aclweb.org/anthology/P13-4028.pdf
    http://www.semanticsimilarity.org

    Stanford?
    Apache lucene


To find if it is a question
https://stackoverflow.com/questions/13027908/stanford-parser-tags (verified in corenlp.run also with What and Is questions)
     */
}
