package com.example.demo.process;

import com.example.demo.Entity.Question;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.Set;

@Component
public class QuestionProcessor {
    @Autowired
    private Environment env;
    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init(){
        Properties props = new Properties();
        props.setProperty("annotators", env.getProperty("corenlp.annotators"));
        // use faster shift reduce parser
        props.setProperty("parse.model", env.getProperty("corenlp.parse.model"));
        props.setProperty("parse.maxlen", env.getProperty("corenlp.parse.maxlen"));
        pipeline = new StanfordCoreNLP(props);
    }

    public Question processQuestion(Question question) {
        // build annotation for a review
        Annotation annotation = new Annotation(question.getQuestionString());
        pipeline.annotate(annotation);
        // get tree
        Tree tree =
                annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
        System.out.println(tree);
        Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
        for (Constituent constituent : treeConstituents) {
            if (constituent.label() != null &&
                    (constituent.label().toString().equals("VP") || constituent.label().toString().equals("NP"))) {
                System.out.println("found constituent: " + constituent.toString());
                System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end() + 1));
            }
        }
        Question processedQuestion = new Question();
        processedQuestion.setQuestionString(question.getQuestionString());
        processedQuestion.setTree(tree.toString());
        if (processedQuestion.getTree().contains("SBARQ")|processedQuestion.getTree().contains("SQ")) {
            processedQuestion.setIsQuestion(true);
        }
        else {
            processedQuestion.setIsQuestion(false);
        }
        return processedQuestion;
    }
}