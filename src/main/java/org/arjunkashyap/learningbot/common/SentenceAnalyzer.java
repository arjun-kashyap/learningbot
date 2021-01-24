package org.arjunkashyap.learningbot.common;

import edu.stanford.nlp.pipeline.CoreEntityMention;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import org.arjunkashyap.learningbot.Entity.BotPOS;
import org.arjunkashyap.learningbot.Entity.BotWord;
import org.arjunkashyap.learningbot.Entity.Question;
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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SentenceAnalyzer implements InitializingBean{
    @Autowired
    BotProperties props;
    private StanfordCoreNLP pipeline;
    private StanfordCoreNLP pipelineAlt;
    @Autowired
    WordnetUtils wordnetUtils;

    public static void main(String[] args) throws JWNLException {//TODO: OK: Remove this
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,lemma,ner");
        props.setProperty("ner.applyFineGrained", "false");
        props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        props.setProperty("parse.maxlen", "100");
        SentenceAnalyzer s = new SentenceAnalyzer();
        s.pipeline = new StanfordCoreNLP(props);
        List<BotWord> x = s.getPosElements("Did John and their young mother aged 60  die of pneumonia?");
        System.out.println(x);
    }

    public Question processQuestion(String questionString) {
        Annotation annotation = new Annotation(questionString);

        Question processedQuestion = process(pipeline, annotation);
        processedQuestion.setParser("SR");
        if (!processedQuestion.getIsQuestion()) {
            //Trying fallback parser
            System.out.println("SR Parser did not detect it as question. Using PCFG parser.");
            processedQuestion = process(pipelineAlt, annotation);
            processedQuestion.setParser("PCFG");
        }
        processedQuestion.setQuestionString(questionString);

        CoreDocument document = pipeline.processToCoreDocument(questionString);
        List<CoreLabel> tokens = document.tokens();
        StringBuilder cleanedQuestion = new StringBuilder();
        if (tokens.size() > 0) {
            for (CoreLabel tok : tokens) {
                cleanedQuestion.append(" "+tok.word());
            }
        }
        processedQuestion.setCleanedQuestionString(cleanedQuestion.toString().trim());

        return processedQuestion;
    }

    private Question process(StanfordCoreNLP pipeline, Annotation annotation) {
        Question processedQuestion = new Question();
        pipeline.annotate(annotation);
        List<CoreMap> coreMapList = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (coreMapList.size() >= 1) {
            Tree tree = coreMapList.get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
/*
            Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());

            for (Constituent constituent : treeConstituents) {
                if (constituent.label() != null &&
                        (constituent.label().toString().equals("VP") || constituent.label().toString().equals("NP"))) {
                }
            }
*/
            processedQuestion.setTree(tree.toString());
 //           System.out.println(tree.toString());
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

    public List<BotWord> getPosElements(String text) throws JWNLException {
        List<BotWord> classifiedList = new ArrayList<>();
        CoreDocument document = pipeline.processToCoreDocument(text);
        BotWord botWord, previousBotWord;
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
                        botWord = new BotWord();
                        botWord.setLemma(entity.text().toLowerCase());
                        botWord.setWord(entity.text().toLowerCase());
                        botWord.setPos(BotPOS.ENTITY);
                        classifiedList.add(botWord);
                        entitiesList.remove(entity);
                        //System.out.println("Removing "+tok.lemma()+" ");
                    }
                } else if (tok.tag().equals("IN") || tok.tag().equals("TO")) { //Take care of "preposition"
                    if (classifiedList.size() >= 1) { //TODO: Check this
                        previousBotWord = classifiedList.get(classifiedList.size() - 1);
                    }
                    else {
                        previousBotWord = null;
                    }
                    if  ((previousBotWord != null) && (previousBotWord.getPos() == BotPOS.NOUN)) {
                        BotWord verb = wordnetUtils.getTopVerbForNounWithPreposition(tok.lemma());
                        if (verb != null) {
                            previousBotWord.setWord(verb.getWord());
                            previousBotWord.setLemma(verb.getLemma());
                            previousBotWord.setPos(BotPOS.VERB);
                        }
                    }
                } else if (tok.tag().equals("RP")) { //Take care of "particle"/. E.g. hang out in Q# 21
                    previousBotWord = classifiedList.get(classifiedList.size() - 1);
                    if  ((previousBotWord != null) && (previousBotWord.getPos() == BotPOS.VERB)) {
                        IndexWord verb = wordnetUtils.wordInWordNet(BotPOS.VERB, previousBotWord.getLemma() + " " + tok.lemma());
                        if (verb != null) {
                            previousBotWord.setWord(previousBotWord.getWord() + " " + tok.word());
                            previousBotWord.setLemma(previousBotWord.getLemma() + " " + tok.lemma());
                        }
                    }
                } else if (!EnglishAnalyzer.getDefaultStopSet().contains(tok.lemma()) && ((
                                    tok.tag().startsWith("NN")||
                                    tok.tag().startsWith("VB") ||
                                    tok.tag().equals("CD") ||
                                    tok.tag().startsWith("JJ") ||
                                    tok.tag().startsWith("RB") ||
                                    tok.tag().startsWith("W")))) { // ||
                    botWord = new BotWord();
                    botWord.setLemma(tok.lemma().toLowerCase());
                    botWord.setWord(tok.word().toLowerCase());
                    classifiedList.add(botWord);
                    if (tok.tag().startsWith("W")) {
                        botWord.setPos(BotPOS.WH_QUESTION);
                        whWordFound = true;
                        if (tok.lemma().toLowerCase().equals("be")) {
                            botWord.setLemma("be-question");
                        }
                    } else if (tok.tag().startsWith("NN")) {
                        botWord.setPos(BotPOS.NOUN);
                    } else if (tok.tag().startsWith("VB")) {
                        botWord.setPos(BotPOS.VERB);
                    } else if (tok.tag().equals("CD")) {// CDs are not coming after we added NER
                        botWord.setPos(BotPOS.CARDINAL_NUMBER);
                    } else if (tok.tag().startsWith("JJ")){
                        botWord.setPos(BotPOS.ADJECTIVE);
                    } else if (tok.tag().startsWith("RB")){
                        botWord.setPos(BotPOS.ADVERB);
                    }
                }
            }

            if (!whWordFound && classifiedList.size() > 0) { // Use the first token as the WH clause
                CoreLabel tok = tokens.get(0);
                if (tok.lemma().equals(classifiedList.get(0).getLemma())) {
                    botWord = classifiedList.get(0);
                } else { // first token did not get added to the list due to stop list etc
                    botWord = new BotWord();
                    botWord.setLemma(tok.lemma().toLowerCase());
                    botWord.setWord(tok.word().toLowerCase());
                    if (tok.lemma().toLowerCase().equals("be")) {
                        botWord.setLemma("be-question");
                    }
                    classifiedList.add(0, botWord);
                }
                botWord.setPos(BotPOS.WH_QUESTION);
            }
        }
        System.out.println(classifiedList);
        return classifiedList;
    }

    @Override
    public void afterPropertiesSet() {
        pipeline = new StanfordCoreNLP(props);
        props.setProperty("parse.model", props.getProperty("parse.model.alt"));
        pipelineAlt = new StanfordCoreNLP(props);
        System.out.println(pipeline.getProperties());
        System.out.println(pipelineAlt.getProperties());
    }
}