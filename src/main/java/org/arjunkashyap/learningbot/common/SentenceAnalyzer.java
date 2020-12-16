package org.arjunkashyap.learningbot.common;

import edu.stanford.nlp.pipeline.CoreEntityMention;
import org.arjunkashyap.learningbot.Entity.BotPOS;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.Entity.Word;
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

@Component
public class SentenceAnalyzer implements InitializingBean {
    @Autowired
    private Environment env;
    private StanfordCoreNLP pipeline;

    public static void main(String[] args) {//TODO: Remove this
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,lemma,ner");
        props.setProperty("ner.applyFineGrained", "false"); //TODO: get from properties
        props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        props.setProperty("parse.maxlen", "100");
        SentenceAnalyzer s = new SentenceAnalyzer();
        s.pipeline = new StanfordCoreNLP(props);
        List<Word> x = s.getPosElements("Did John and their young mother aged 60  die of pneumonia?");
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

    public List<Word> getPosElements(String text) {
        List<Word> classifiedList = new ArrayList<>();
        CoreDocument document = pipeline.processToCoreDocument(text);
        Word word;
        Word previousNoun = null;
        boolean whWordFound = false;

        List<CoreEntityMention> entitiesList = document.entityMentions();
        Map<CoreLabel, CoreEntityMention> labelEntityMap = new HashMap<>();
        for (CoreEntityMention em : entitiesList) { //Named entities. Some issue. "His" is marked as Entity!
            for (CoreLabel tok : em.tokens()) {
                labelEntityMap.put(tok, em);
            }
        }

        List<CoreLabel> tokens = document.tokens();
        if (tokens.size() > 0) {
            for (CoreLabel tok : tokens) {
                if (labelEntityMap.get(tok) != null) {
                    CoreEntityMention entity = labelEntityMap.get(tok);
                    if (entitiesList.contains(entity)) { //else already added a multi-word entity
                        System.out.println("Adding entity " + entity.text());//entity put
                        word = new Word();
                        word.setLemma(entity.text().toLowerCase());
                        word.setWord(entity.text().toLowerCase());
                        word.setPos(BotPOS.ENTITY);
                        classifiedList.add(word);
                        entitiesList.remove(entity);
                        //System.out.println("Removing "+tok.lemma()+" ");
                    }
                    previousNoun = null;
                } else if ((tok.tag().equals("IN") || tok.tag().equals("TO")) &&
                        (previousNoun != null) &&
                        (previousNoun.getPos() == BotPOS.NOUN)) { //Take care of "preposition"
                    previousNoun.setPos(BotPOS.DERIVE_VERB);
                    previousNoun = null;
                } else if (!EnglishAnalyzer.getDefaultStopSet().contains(tok.lemma()) && ((
                                    tok.tag().startsWith("NN")||
                                    tok.tag().startsWith("VB") ||
                                    tok.tag().equals("CD") ||
                                    tok.tag().startsWith("JJ") ||
                                    tok.tag().startsWith("RB") ||
                                    tok.tag().startsWith("W")))) { // ||
                    word = new Word();
                    word.setLemma(tok.lemma().toLowerCase());
                    word.setWord(tok.word().toLowerCase());
                    classifiedList.add(word);
                    previousNoun = null;
                    if (tok.tag().startsWith("W")) {
                        word.setPos(BotPOS.WH_QUESTION);
                        whWordFound = true;
                        if (tok.lemma().toLowerCase().equals("be")) {
                            word.setLemma("be-question");
                        }
                    } else if (tok.tag().startsWith("NN")) {
                        word.setPos(BotPOS.NOUN);
                        previousNoun = word;
                    } else if (tok.tag().startsWith("VB")) {
                        word.setPos(BotPOS.VERB);
                    } else if (tok.tag().equals("CD")) {// CDs are not coming after we added NER
                        word.setPos(BotPOS.CARDINAL_NUMBER);
                    } else if (tok.tag().startsWith("JJ")){
                        word.setPos(BotPOS.ADJECTIVE);
                    } else if (tok.tag().startsWith("RB")){
                        word.setPos(BotPOS.ADVERB);
                    }
                } else {
                    previousNoun = null;
                }
            }

            if (!whWordFound && classifiedList.size() > 0) { // Use the first token as the WH clause
                CoreLabel tok = tokens.get(0);
                if (tok.lemma().equals(classifiedList.get(0).getLemma())) {
                    word = classifiedList.get(0);
                } else { // first token did not get added to the list due to stop list etc
                    word = new Word();
                    word.setLemma(tok.lemma().toLowerCase());
                    word.setWord(tok.word().toLowerCase());
                    if (tok.lemma().toLowerCase().equals("be")) {
                        word.setLemma("be-question");
                    }
                    classifiedList.add(0, word);
                }
                word.setPos(BotPOS.WH_QUESTION);
            }
        }
        //System.out.println(classifiedList);
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