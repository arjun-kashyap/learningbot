package org.arjunkashyap.learningbot.common;

import org.arjunkashyap.learningbot.Entity.Synonym;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordNetUtilities {
    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */
    public static Set<Synonym> getTopSynonyms(String inputWord, POS inputPos) {
        int limit = 20;
        Set<Synonym> synonyms = new HashSet<>();
        Set<Synonym> hypernyms = new HashSet<>();
        Set<Synonym> hyponyms = new HashSet<>();

        Synonym mainWord = new Synonym();
        mainWord.setWord(inputWord);
        mainWord.setScore(100);
        mainWord.setLinkType(Link.also);
        mainWord.setPos(inputPos);
        synonyms.add(mainWord);

        for (String synsetId : getSynsetsForWord(inputWord, inputPos)) { //Get other words in the synsets of the word
            synonyms.addAll(getWordsInSynset(synsetId, Link.syns, inputPos));
            for (String hyperSynsetId : getHyperSynsets(synsetId)) { //Get hyper (one level up) words
                hypernyms.addAll(getWordsInSynset(hyperSynsetId, Link.hype, inputPos));
                for (String hypoSynsetId : getHypoSynsets(hyperSynsetId)) { //Get hypo of hyper i.e same level words
                    hyponyms.addAll(getWordsInSynset(hypoSynsetId, Link.hypo, inputPos));
                }
            }
        }

        if (synonyms.size() < limit) {
            synonyms.addAll(hypernyms);
        }
        if (synonyms.size() < limit) {
            synonyms.addAll(hyponyms);
        }
        return synonyms;
    }

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

    private static List<Synonym> getWordsInSynset(String synsetId, Link linkType, POS inputPos) {
        List<Synonym> synonyms = new ArrayList<>();
        List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hype);
        for (Synlink synlink : synlinks) {
            List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(synlink.getSynset1(), Lang.eng);
            for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                Synonym synonym = new Synonym();
                synonym.setWord(foundWord.getLemma().replace('_', ' ')); //Wordnet gives multi-word synonyms with _
                synonym.setLinkType(linkType);
                synonym.setPos(inputPos);
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
}
