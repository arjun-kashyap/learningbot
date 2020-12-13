package Scratch.ws4j;

import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.PorterStemmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.dictionary.Dictionary;
import org.arjunkashyap.learningbot.Entity.Synonym;

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
                System.out.println("INSIDE SENSES");
                String synsetId = senseOfInputWord.getSynset();
//                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
                for (Synlink synlink : synlinks) {
                    System.out.println("INSIDE SYNLINKS: "+synlink.getLink());
                    String l;
                    if (synlink.getLink() == Link.hype)
                        l = synlink.getSynset1();
                    else
                        l = synlink.getSynset2();//ss
                    List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(l, Lang.eng);
                    for (Sense senseOfFoundSynset : sensesForFoundSynset) {
                        Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());

                        System.out.print(foundWord.getLemma()+" ");
                        if (foundWord.getPos() == inputPos) {
                            Relatedness s = rc.calcRelatednessOfSynset(new Concept(synsetId, inputPos), new Concept(synlink.getSynset2(), inputPos));
                            Synonym synonym = new Synonym();
                            synonym.setWord(foundWord.getLemma());
                            synonym.setScore((int) Math.round(s.getScore()*100));
                            synonym.setLinkType(synlink.getLink());
                            synonyms.add(synonym);
                        }
                    }
                    System.out.println("");
                }
            }
        }
        synonyms.sort(Collections.reverseOrder());
        if (synonyms.size()>0)
            return synonyms.subList(0,Math.min(900, synonyms.size()-1));
        else
            return synonyms;
    }

    public static void main(String[] args) {//Gives only same level
        List<Synonym> x = getTopSynonyms("meaning", POS.n);
        for (Synonym a : x) {
            System.out.println(a.getWord());
        }

    }
}
