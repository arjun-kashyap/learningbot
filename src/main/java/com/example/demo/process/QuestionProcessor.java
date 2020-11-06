package com.example.demo.process;

import com.example.demo.Entity.Question;
import com.example.demo.Entity.WordClassification;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Component
public class QuestionProcessor implements InitializingBean {
    @Autowired
    private Environment env;
    private StanfordCoreNLP pipeline;

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
        if (processedQuestion.getTree().contains("SBARQ")||processedQuestion.getTree().contains("SQ")) {
            processedQuestion.setIsQuestion(true);
        }
        else {
            processedQuestion.setIsQuestion(false);
        }
        return processedQuestion;
    }

    public List<WordClassification> getPosElements(String text) {
        List<WordClassification> classifiedList = new ArrayList<>();
        CoreDocument document = pipeline.processToCoreDocument(text);
        for (CoreLabel tok : document.tokens()) {
            CharArraySet x = EnglishAnalyzer.getDefaultStopSet();//TODO: stop words
            if ((tok.tag().startsWith("NN") || tok.tag().startsWith("VB"))) {
                WordClassification wc = new WordClassification();
                wc.setLemma(tok.lemma());
                wc.setWord(tok.word());
                wc.setPos(tok.tag());
                classifiedList.add(wc);
            }
        }
        return classifiedList;
    }

    @Override
    public void afterPropertiesSet()  {
        Properties props = new Properties();
        props.setProperty("annotators", env.getProperty("corenlp.annotators"));
        // use faster shift reduce parser
        props.setProperty("parse.model", env.getProperty("corenlp.parse.model"));
        props.setProperty("parse.maxlen", env.getProperty("corenlp.parse.maxlen"));
        pipeline = new StanfordCoreNLP(props);
    }
}