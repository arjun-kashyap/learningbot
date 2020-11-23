package com.example.demo.Scratch.ws4j;

import com.example.demo.Entity.Synonym;
import com.example.demo.Entity.SynsetsWords;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAPIDemoQueue {
    public AdvancedAPIDemoQueue() {
    }

    static ILexicalDatabase db = new NictWordNet();
    static RelatednessCalculator rc = new WuPalmer(db);

    static {
        WS4JConfiguration.getInstance().setMFS(true);
    }
    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */


    private static SynsetsWords getSynonyms(String inputWord, POS inputPos) {
        return getRelatedWords(inputWord, inputPos, Link.hype, Link.syns);
    }

    private static SynsetsWords getHypernyms(String inputWord, POS inputPos) {
        return getRelatedWords(inputWord, inputPos, Link.hype, Link.hype);
    }

    private static SynsetsWords getSiblings(String inputWord, POS inputPos) {
        return getRelatedWords(inputWord, inputPos, Link.hypo, Link.hypo);
    }

    private static SynsetsWords getRelatedWords(String inputWord, POS inputPos, Link linkType, Link relationshipType) {
        List<Synonym> synonyms = new ArrayList<>();
        List<String> synsets = new ArrayList<>();
        List<Word> words = WordDAO.findWordsByLemmaAndPos(inputWord, inputPos);
        for (Word word : words) {
            List<Sense> sensesOfInputWord = SenseDAO.findSensesByWordid(word.getWordid());
            for (Sense senseOfInputWord : sensesOfInputWord) {
                String synsetId = senseOfInputWord.getSynset();
                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, linkType);
                for (Synlink synlink : synlinks) {
                    String linkedSynsetId;
                    if (linkType == Link.hype)
                        linkedSynsetId = synlink.getSynset1();
                    else
                        linkedSynsetId = synlink.getSynset2();
                    List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(linkedSynsetId, Lang.eng);
                    synsets.add(linkedSynsetId);
                    for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                        Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                        Synonym synonym = new Synonym();
                        synonym.setWord(foundWord.getLemma());
                        synonym.setLinkType(relationshipType);
                        synonym.setPos(foundWord.getPos());
                        synonyms.add(synonym);
                    }
                }
            }
        }
        SynsetsWords synsetsWords = new SynsetsWords();
        synsetsWords.setSynonyms(synonyms);
        synsetsWords.setSynsets(synsets);
        return synsetsWords;
    }

public static SynsetsWords x(String synsetId, Link linkType, Link relationshipType) {
    List<Synonym> synonyms = new ArrayList<>();
    List<String> synsets = new ArrayList<>();
    List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, linkType);
    for (Synlink synlink : synlinks) {
        String linkedSynsetId;
        if (linkType == Link.hype)
            linkedSynsetId = synlink.getSynset1();
        else
            linkedSynsetId = synlink.getSynset2();
        synsets.add(linkedSynsetId);
        List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(linkedSynsetId, Lang.eng);
        for (Sense senseOfFoundSynset : sensesForFoundSynset) {
            Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
            Synonym synonym = new Synonym();
            synonym.setWord(foundWord.getLemma());
            synonym.setLinkType(relationshipType);
            synonym.setPos(foundWord.getPos());
            synonyms.add(synonym);
        }
    }
    SynsetsWords synsetsWords = new SynsetsWords();
    synsetsWords.setSynonyms(synonyms);
    synsetsWords.setSynsets(synsets);
    return synsetsWords;
}

    public static void main(String[] args) {//Gives only same level
        List<Synonym> synonyms = null;
        List<Synonym> hypernyms = null;
        List<Synonym> siblings = null;
        SynsetsWords tmp = null;
        int limit = 1000;
        tmp = getSynonyms("phone", POS.n);
        synonyms = tmp.getSynonyms();

        if (synonyms.size()<limit) {
            for (String synset:tmp.getSynsets()) {
                tmp = x(synset, Link.hype, Link.hype);
                hypernyms = tmp.getSynonyms();
            }
        }

        if (hypernyms != null && synonyms.size()+hypernyms.size() <limit) {
            for (String synset:tmp.getSynsets()) {
                tmp = x(synset, Link.hypo, Link.hypo);
                siblings = tmp.getSynonyms();
            }
        }

        System.out.println("SYNONYMS");
        for (Synonym a : synonyms) {
            //synonym.setScore((int) Math.round(s.getScore()*100));
            System.out.println(a.getWord());
        }

        System.out.println("HYPER");
        for (Synonym a : hypernyms) {
            //synonym.setScore((int) Math.round(s.getScore()*100));
            System.out.println(a.getWord());
        }

        System.out.println("SIBLINGS");
        for (Synonym a : siblings) {
            //synonym.setScore((int) Math.round(s.getScore()*100));
            System.out.println(a.getWord());
        }

    }
}
