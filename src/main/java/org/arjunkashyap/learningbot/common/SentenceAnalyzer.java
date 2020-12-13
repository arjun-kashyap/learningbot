package org.arjunkashyap.learningbot.common;

import edu.cmu.lti.ws4j.util.PorterStemmer;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import org.arjunkashyap.learningbot.Entity.BotPOS;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.Entity.WordClassification;
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

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SentenceAnalyzer implements InitializingBean {
    @Autowired
    private Environment env;
    private StanfordCoreNLP pipeline;

    /*

     */

    public static void main(String[] args) {//TODO: Remove this
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,lemma,ner");
        props.setProperty("ner.applyFineGrained", "false"); //TODO: get from properties
        props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        props.setProperty("parse.maxlen", "100");
        SentenceAnalyzer s = new SentenceAnalyzer();
        s.pipeline = new StanfordCoreNLP(props);
        List<WordClassification> x = s.getPosElements("What is the meaning of word duck?");
        System.out.println(x);
    }

    public Question processQuestion(String questionString) {
        Question processedQuestion = new Question();
        processedQuestion.setQuestionString(questionString);
        Annotation annotation = new Annotation(questionString);
        pipeline.annotate(annotation);
        List<CoreMap> coreMapList = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (coreMapList.size() >= 1) {
            Tree tree = coreMapList.get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
            Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
            for (Constituent constituent : treeConstituents) {
                if (constituent.label() != null &&
                        (constituent.label().toString().equals("VP") || constituent.label().toString().equals("NP"))) {
                }
            }
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
        WordClassification previousWord = null;
        boolean whWordFound = false;
        List<CoreLabel> tokens = document.tokens();
        if (tokens.size() > 0) {
            for (CoreLabel tok : tokens) {
                if ((tok.tag().equals("IN") || tok.tag().equals("TO")) && (previousWord != null) && (previousWord.getPos().startsWith("NN"))) { //Take care of "preposition"
                    previousWord.setPos("X-DERVB"); //Derivationally related form
                }
              /* if (!tok.ner().equals("O") && !tok.tag().equals("CD")) { //Named entity, skip. Don't skip CD
                    previousWord = null; //TODO: check it? Added it without much thought :)
                } else {

                */
                if (!EnglishAnalyzer.getDefaultStopSet().contains(tok.lemma()) && ((tok.tag().startsWith("NN") || tok.tag().startsWith("VB") || tok.tag().startsWith("W")))) { //tok.tag().equals("CD") ||
                    word = new WordClassification();
                    word.setLemma(tok.lemma().toLowerCase());
                    word.setWord(tok.word().toLowerCase());
                    word.setPos(tok.tag());
                    classifiedList.add(word);
                    if (tok.tag().startsWith("W")) {
                        whWordFound = true;
                        previousWord = null;
                    } else if (tok.tag().startsWith("NN")) {
                        word.setPos("NN");
                        previousWord = word;
                    } else {
                        previousWord = null;
                    }
                } else {
                    previousWord = null;
                }
                //}
            }

            if (!whWordFound && classifiedList.size() > 0) { // Use the first token as the WH clause
                CoreLabel tok = tokens.get(0);
                if (tok.lemma().equals(classifiedList.get(0).getLemma())) {
                    word = classifiedList.get(0);
                } else { // first token did not get added to the list due to stop list etc
                    word = new WordClassification();
                    word.setLemma(tok.lemma().toLowerCase());
                    word.setWord(tok.word().toLowerCase());
                    word.setPos("W-DERIVED");
                    classifiedList.add(0, word);
                }
                word.setPos("W-DERIVED");
            }
        }

        for (CoreEntityMention em : document .entityMentions()) { //Named entities
            for (CoreLabel tok: em.tokens()) {
                word = new WordClassification();
                word.setLemma(tok.lemma().toLowerCase());
                word.setWord(tok.word().toLowerCase());
                word.setPos("CD"); //check if detected as a CD before
                if (!classifiedList.contains(word)) {
                    word.setPos("NN");
                    if (classifiedList.contains(word)) {
                        classifiedList.get(classifiedList.indexOf(word)).setPos("ENTITY");
                    } else {
                        word.setPos("X-DERVB");
                        if (classifiedList.contains(word)) {
                            classifiedList.get(classifiedList.indexOf(word)).setPos("ENTITY");
                        } else {
                            // Not sure why we will come here
                            System.out.println("TODO: check why we came here: " + word.getLemma() + word.getPos());
                            word.setPos("ENTITY");
                            classifiedList.add(word);
                        }
                    }
                }
            }
        }
        System.out.println(classifiedList);
        return classifiedList;
    }

    @Override
    public void afterPropertiesSet() {
        Properties props = new Properties();
        props.setProperty("annotators", env.getProperty("corenlp.annotators"));
        props.setProperty("ner.applyFineGrained", "false"); //TODO: get from properties
        props.setProperty("parse.model", env.getProperty("corenlp.parse.model"));
        props.setProperty("parse.maxlen", env.getProperty("corenlp.parse.maxlen"));
        pipeline = new StanfordCoreNLP(props);
    }
}