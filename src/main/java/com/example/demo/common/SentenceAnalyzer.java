package com.example.demo.common;

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
import edu.stanford.nlp.util.CoreMap;
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
public class SentenceAnalyzer implements InitializingBean {
    @Autowired
    private Environment env;
    private StanfordCoreNLP pipeline;

    public Question processQuestion(Question question) {
        // build annotation for a review
        Question processedQuestion = new Question();
        Annotation annotation = new Annotation(question.getQuestionString());
        pipeline.annotate(annotation);
        // get tree
        List<CoreMap> coreMapList = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (coreMapList.size() >= 1) {
            Tree tree = coreMapList.get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
            Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
            for (Constituent constituent : treeConstituents) {
                if (constituent.label() != null &&
                        (constituent.label().toString().equals("VP") || constituent.label().toString().equals("NP"))) {
                }
            }
            processedQuestion.setQuestionString(question.getQuestionString());
            processedQuestion.setTree(tree.toString());
            if (processedQuestion.getTree().contains("SBARQ") || processedQuestion.getTree().contains("SQ")) {
                processedQuestion.setIsQuestion(true);
            } else {
                processedQuestion.setIsQuestion(false);
            }
        } else {
            processedQuestion.setIsQuestion(false);
        }

        return processedQuestion;
    }

    public List<WordClassification> getPosElements(String text) {
        List<WordClassification> classifiedList = new ArrayList<>();
        CoreDocument document = pipeline.processToCoreDocument(text);
        WordClassification word;
        WordClassification lastWord = null;
        for (CoreLabel tok : document.tokens()) {
            if (!EnglishAnalyzer.getDefaultStopSet().contains(tok.lemma()) && ((tok.tag().startsWith("NN") || tok.tag().startsWith("VB")))) {
                word = new WordClassification();
                word.setLemma(tok.lemma());
                word.setWord(tok.word());
                word.setPos(tok.tag());
                classifiedList.add(word);
                lastWord = word;
            }
            if ((tok.tag().equals("IN") || tok.tag().equals("TO"))&&(lastWord != null)&&(lastWord.getPos().startsWith("NN"))) { //Take care of "preposition"
                lastWord.setPos("VB");
                lastWord = null;
            }
        }
        return classifiedList;
    }

    @Override
    public void afterPropertiesSet() {
        Properties props = new Properties();
        props.setProperty("annotators", env.getProperty("corenlp.annotators"));
        // use faster shift reduce parser
        props.setProperty("parse.model", env.getProperty("corenlp.parse.model"));
        props.setProperty("parse.maxlen", env.getProperty("corenlp.parse.maxlen"));
        pipeline = new StanfordCoreNLP(props);
    }
}