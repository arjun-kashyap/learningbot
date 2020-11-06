package com.example.demo.Controller;

import com.example.demo.Entity.BotResponse;
import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import com.example.demo.process.AnsweringMachine;
import com.example.demo.process.QuestionIndexer;
import com.example.demo.process.QuestionProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RestActionsController {
    @Autowired
    private AnsweringMachine answeringMachine;
    @Autowired
    private QuestionProcessor questionProcessor;
    @Autowired
    private QuestionIndexer questionIndexer;

    @PostMapping(
            value = "/postQuestion", consumes = "application/json", produces = "application/json")
    public BotResponse postQuestion(@RequestBody Question question) {
        List<Match> matches;
        question = questionProcessor.processQuestion(question);
        matches = answeringMachine.getAnswer(question);

        BotResponse botResponse = new BotResponse();
        botResponse.setTopAnswer(matches.get(0).getAnswer());
        botResponse.setMatchList(matches);
        botResponse.setQuestionDecomposed(question);
        return botResponse;
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
