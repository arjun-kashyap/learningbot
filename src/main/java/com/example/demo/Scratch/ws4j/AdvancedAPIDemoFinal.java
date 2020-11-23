package com.example.demo.Scratch.ws4j;

import com.example.demo.Entity.Synonym;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import java.util.*;

public class AdvancedAPIDemoFinal {

    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */

    private static List<String> getSynsetsForWord(String inputWord, POS inputPos) {
        List<String> synsets = new ArrayList<>();
        List<Word> words = WordDAO.findWordsByLemmaAndPos(inputWord, inputPos);
        for (Word word : words) {
            List<Sense> sensesOfInputWord = SenseDAO.findSensesByWordid(word.getWordid());
            for (Sense senseOfInputWord : sensesOfInputWord) {
                String synsetId = senseOfInputWord.getSynset();
                synsets.add(synsetId);
            }
        }
        return synsets;
    }

    private static List<Synonym> getWordsInSynset(String synsetId, Link linkType) {
        List<Synonym> synonyms = new ArrayList<>();
        List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hype);
        for (Synlink synlink : synlinks) {
            List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(synlink.getSynset1(), Lang.eng);
            for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                Synonym synonym = new Synonym();
                synonym.setWord(foundWord.getLemma());
                synonym.setLinkType(linkType);
                synonyms.add(synonym);
            }
        }
        return synonyms;
    }

    private static List<String> getHyperSynsets(String synsetId) { //One level down
        List<String> synsets = new ArrayList<>();
        for (Synlink synlink : SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hype)) {
            synsets.add(synlink.getSynset2());
        }
        return synsets;
    }

    private static List<String> getHypoSynsets(String synsetId) {//One level up
        List<String> synsets = new ArrayList<>();
        for (Synlink synlink : SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hypo)) {
            synsets.add(synlink.getSynset2());
        }
        return synsets;
    }

    public static void main(String[] args) {
        String inputWord = "phone";
        POS inputPos = POS.n;
        int limit = 1000;

        Set<String> sameLevelSynsets = new HashSet<>();
        Set<String> allHyperSynsets = new HashSet<>();
        Set<String> allHypoSynsets = new HashSet<>();

        Set<Synonym> synonyms = new HashSet<>();
        Set<Synonym> hypernyms = new HashSet<>();
        Set<Synonym> hyponyms = new HashSet<>();

        //Get other words at the same level
        Synonym mainWord = new Synonym();
        mainWord.setWord(inputWord);
        mainWord.setScore(100);
        mainWord.setPos(inputPos);
        synonyms.add(mainWord);
        sameLevelSynsets.addAll(getSynsetsForWord(inputWord, inputPos));
        for (String synsetId : sameLevelSynsets) {
            synonyms.addAll(getWordsInSynset(synsetId, Link.syns));
            for (String hyperSynsetId : getHyperSynsets(synsetId)) {
                hypernyms.addAll(getWordsInSynset(hyperSynsetId, Link.hype));
                for (String hypoSynsetId : getHypoSynsets(hyperSynsetId)) {
                    hyponyms.addAll(getWordsInSynset(hypoSynsetId, Link.hypo));
                }
            }
        }

//-------------------------------------------------
        System.out.println("Same Level count: " + synonyms.size());
        for (Synonym x : synonyms) {
            System.out.println(x.getWord()+" "+x.getScore());
        }

        System.out.println("Up Level count: " + hypernyms.size());
        for (Synonym x : hypernyms) {
            System.out.println(x.getWord()+" "+x.getScore());
        }

        System.out.println("Down Level count: " + hyponyms.size());
        for (Synonym x : hyponyms) {
            System.out.println(x.getWord()+" "+x.getScore());
        }

        if (synonyms.size() < limit) {
            synonyms.addAll(hypernyms);
        }
        if (synonyms.size() < limit) {
            synonyms.addAll(hyponyms);
        }
        //return synonyms;
    }
}
