package com.example.demo.Scratch.ws4j;

import com.example.demo.Entity.Synonym;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedAPIDemoLite {
    public AdvancedAPIDemoLite() {
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

    private static List<Synonym> getTopSynonyms(String inputWord, POS inputPos) {

        List<Word> words = WordDAO.findWordsByLemmaAndPos(inputWord, inputPos);
        List<Synonym> synonyms = new ArrayList<>();

        for (Word word : words) {
            List<Sense> sensesOfInputWord = SenseDAO.findSensesByWordid(word.getWordid());
            for (Sense senseOfInputWord : sensesOfInputWord) {
                String synsetId = senseOfInputWord.getSynset();
                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
                for (Synlink synlink : synlinks) {
                    List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(synlink.getSynset2(), Lang.eng);
                    for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                        Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                        if (foundWord.getPos() == inputPos) {
                            Relatedness s = rc.calcRelatednessOfSynset(new Concept(synsetId, inputPos), new Concept(synlink.getSynset2(), inputPos));
                            Synonym synonym = new Synonym();
                            synonym.setWord(foundWord.getLemma());
                            synonym.setScore((int) Math.round(s.getScore()*100));
                            synonym.setLinkType(synlink.getLink());
                            synonyms.add(synonym);
                        }
                    }
                }
            }
        }
        synonyms.sort(Collections.reverseOrder());
        if (synonyms.size()>0)
            return synonyms.subList(0,Math.min(9, synonyms.size()-1));
        else
            return synonyms;
    }

    public static void main(String[] args) {
        System.out.println(getTopSynonyms("abraham_lincoln", POS.n));
    }
}
