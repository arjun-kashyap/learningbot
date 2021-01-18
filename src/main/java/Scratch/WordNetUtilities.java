package Scratch;

import org.arjunkashyap.learningbot.Entity.BotPOS;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import org.arjunkashyap.learningbot.Entity.BotWord;

import java.util.*;

public class WordNetUtilities {
    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */
    public static List<BotWord> getTopSynonyms(String inputWord, BotPOS inputPos, int limit) {
        List<BotWord> synonyms = new ArrayList<>();
        List<BotWord> hypernyms = new ArrayList<>();
        List<BotWord> hyponyms = new ArrayList<>();

        BotWord mainBotWord = new BotWord();
        mainBotWord.setWord(inputWord);
        mainBotWord.setLinkType(Link.also);
        mainBotWord.setPos(inputPos);
        synonyms.add(mainBotWord);
        inputWord.replace(" ", "_"); // Wordnet compound words have _

        if (limit > 1 && inputPos != BotPOS.CARDINAL_NUMBER && inputPos != BotPOS.WH_QUESTION) {
            for (String synsetId : getSynsetsForWord(inputWord, inputPos)) { //Get other words in the synsets of the word
                synonyms.addAll(getWordsInSynset(synsetId, Link.syns, inputPos));
                for (String hyperSynsetId : getHyperSynsets(synsetId)) { //Get hyper (one level up) words
                    hypernyms.addAll(getWordsInSynset(hyperSynsetId, Link.hype, inputPos));
                    for (String hypoSynsetId : getHypoSynsets(hyperSynsetId)) { //Get hypo of hyper i.e same level words
                        hyponyms.addAll(getWordsInSynset(hypoSynsetId, Link.hypo, inputPos));
                    }
                }
            }

            if (synonyms.size() < limit && hypernyms.size() > 0) {
                synonyms.addAll(hypernyms.subList(0, Math.min(limit - synonyms.size(), hypernyms.size())));
            }
            if (synonyms.size() < limit && hyponyms.size() > 0) {
                synonyms.addAll(hyponyms.subList(0, Math.min(limit - synonyms.size(), hyponyms.size())));
            }
        }
        return synonyms;
    }

    private static List<String> getSynsetsForWord(String inputWord, BotPOS inputPos) {
        List<String> synsets = new ArrayList<>();
        POS pos = null;
        if ((inputPos == BotPOS.NOUN)||(inputPos == BotPOS.ENTITY)) {
            pos = POS.n;
        } else if (inputPos == BotPOS.VERB) {
            pos = POS.v;
        }
        List<edu.cmu.lti.jawjaw.pobj.Word> words = WordDAO.findWordsByLemmaAndPos(inputWord, pos);
        for (edu.cmu.lti.jawjaw.pobj.Word word : words) {
            List<Sense> sensesOfInputWord = SenseDAO.findSensesByWordid(word.getWordid());
            for (Sense senseOfInputWord : sensesOfInputWord) {
                String synsetId = senseOfInputWord.getSynset();
                synsets.add(synsetId);
            }
        }
        return synsets;
    }

    private static List<BotWord> getWordsInSynset(String synsetId, Link linkType, BotPOS inputPos) {
        List<BotWord> synonyms = new ArrayList<>();
        List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hype);
        for (Synlink synlink : synlinks) {
            List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(synlink.getSynset1(), Lang.eng);
            for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                edu.cmu.lti.jawjaw.pobj.Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                BotWord synonym = new BotWord();
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
