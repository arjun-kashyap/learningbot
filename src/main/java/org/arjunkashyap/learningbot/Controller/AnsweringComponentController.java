package org.arjunkashyap.learningbot.Controller;

import org.arjunkashyap.learningbot.Entity.AnswerResponse;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.process.AnswerProcessor;
import org.arjunkashyap.learningbot.process.KnowledgeProcessor;
import org.arjunkashyap.learningbot.common.SentenceAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.TreeSet;

@RestController
public class AnsweringComponentController {
    @Autowired private AnswerProcessor answerProcessor;
    @Autowired private SentenceAnalyzer sentenceAnalyzer;
    @Autowired private KnowledgeProcessor knowledgeProcessor;

    @PostMapping(
            value = "/postQuestion", consumes = "application/json", produces = "application/json")
    public AnswerResponse postQuestion(@RequestBody Question question) {
        TreeSet<Match> matches;
        long startTime = System.currentTimeMillis();
        question = sentenceAnalyzer.processQuestion(question);
        matches = answerProcessor.getAnswer(question);

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
        //TODO: Use NER parser of CoreNLP to detect names of people and organization and use in addition to nouns and verbs
        // See https://www.aclweb.org/anthology/P14-5010.pdf

        answerResponse.setStatus("SUCCESS");
        answerResponse.setDebugInfo(question.toString()+matches.toString()); //TODO: Make debugInfo Json?
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
