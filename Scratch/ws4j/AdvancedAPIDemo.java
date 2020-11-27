package com.example.demo.Scratch.ws4j;

import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.SynsetDAO;
import edu.cmu.lti.jawjaw.db.SynsetDefDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import edu.cmu.lti.lexical_db.data.Concept;

import java.util.ArrayList;
import java.util.List;

public class AdvancedAPIDemo {
    public AdvancedAPIDemo() {
    }

    private static void run(String word, POS pos) {
//        List<Word> words = WordDAO.findWordsByLemmaAndPos(word, pos);
        List<Word> words = WordDAO.findWordsByLemma(word);
        for (Word w : words
        ) {
            if (!w.getPos().equals(pos)) {
                System.out.println("Continuing"+w.getPos());
                //continue;
            }
            List<Sense> senses1 = SenseDAO.findSensesByWordid(((Word) w).getWordid());
            for (Sense s : senses1
            ) {
                String synsetId = s.getSynset();
                System.out.println("ORIG INSIDE SENSES "+ synsetId);
                //List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hypo);
                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
                for (Synlink synlink : synlinks
                ) {
                    System.out.println("ORIG INSIDE SYNLINKS "+synlink.getLink());
                    List<Sense> sensesForLinkedSynset = SenseDAO.findSensesBySynset(synlink.getSynset2());
                    SimilarityCalculationDemo sc1 = new SimilarityCalculationDemo();
                    for (Sense sense : sensesForLinkedSynset
                    ) {
                        if (sense.getLang().toString().contains("eng")) {
                            //List<Word> l = WordDAO.findWordsByLemmaAndPos(WordDAO.findWordByWordid(sense.getWordid()).getLemma(), pos);
                            Word ww = WordDAO.findWordByWordid(sense.getWordid());
                            List<Word> l = new ArrayList<>();
                            l.add(ww);
                            for (Word word1 : l
                            ) {
                             /*   SimilarityCalculationDemo sc = new SimilarityCalculationDemo();
                                System.out.println("========================");
                                System.out.println("SYNLINK2: " + l);
                                sc.run(word, word1.getLemma());
                                Concept c1 = new Concept(synsetId, pos);
                                Concept c2 = new Concept(synlink.getSynset2(), pos);
                                sc1.run(c1, c2);
                                System.out.println("========================");
                            */
                                System.out.print(word1.getLemma()+" ");
                            }
                            System.out.println("");

                        }
                    }
                    //System.out.println("SYNLINK2: "+WordDAO.findWordByWordid(SenseDAO.findSensesBySynset(sy.getSynset2()).get(0).getWordid()));
                }
            }
        }
        // System.out.println(words.get(0));
        // System.out.println(senses.get(0));
        // System.out.println(synset);
        // System.out.println(synsetDef);
        // System.out.println(synlinks.get(0));
    }

    public static void main(String[] args) {//Gives only hyper and hypo. No same level
        run("phone", POS.n);
    }
}
