package org.arjunkashyap.learningbot.common;

import edu.cmu.lti.jawjaw.pobj.Link;
import edu.stanford.nlp.util.EditDistance;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;
import org.arjunkashyap.learningbot.Entity.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WordnetUtils {

    EditDistance ed = new EditDistance();

    private static Dictionary dictionary = null;

    static {
        try {
            dictionary = Dictionary.getDefaultResourceInstance();
        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws JWNLException {
        List<SynonymSynset> synonyms;
        BotWord synonym;
        WordnetUtils wordnetUtils = new WordnetUtils();
        //synonyms = wordnetUtils.getVerbForNounWithPreposition("meaning");
        //synonyms = wordnetUtils.getWordsInSameLevel(wordInWordNet(POS.NOUN, "phone"), BotPOS.noun);
        // synonyms = wordnetUtils.getHypernyms(wordInWordNet(POS.NOUN, "phone"), BotPOS.noun);

        // synonyms = wordnetUtils.getSiblings(wordInWordNet(POS.NOUN, "phone"), BotPOS.noun);
        //synonyms = wordnetUtils.getSiblings(wordInWordNet(POS.VERB, "catch up"), BotPOS.noun);
        //synonyms = wordnetUtils.getTopSynonyms("phone", BotPOS.verb, 10);
        //synonym = wordnetUtils.getTopVerbForNounWithPreposition("president");
        //synonyms = wordnetUtils.getTopSynsets("big", BotPOS.ADJECTIVE, 10000
        //synonyms = wordnetUtils.getTopSynsets("phone", BotPOS.NOUN,1000000);
        //System.out.println(synonyms);
        System.out.println(wordnetUtils.getSynsetsOfWordsInSameLevel(wordInWordNet(BotPOS.VERB, "have"), BotPOS.VERB));
    }

    public static IndexWord wordInWordNet(BotPOS pos, String word) throws JWNLException {
        return dictionary.getIndexWord(convertBotPosToPos(pos), word);
    }

    private static POS convertBotPosToPos(BotPOS botPOS) {
        POS pos = null;
        switch (botPOS) {
            case NOUN:
            case ENTITY:
                pos = POS.NOUN;
                break;
            case VERB:
                pos = POS.VERB;
                break;
            case ADJECTIVE:
                pos = POS.ADJECTIVE;
                break;
            case ADVERB:
                pos = POS.ADVERB;
                break;
        }
        return pos;
    }

    public List<SynonymSynset> getTopSynsets(String inputWord, BotPOS inputPos, int limit) {
        Set<SynonymSynset> synonyms = new HashSet<>();
        if (limit >= 1 && inputPos != null && inputPos != BotPOS.WH_QUESTION && inputPos != BotPOS.CARDINAL_NUMBER) {
            IndexWord indexWord = null;
            try {
                indexWord = wordInWordNet(inputPos, inputWord);
                if (indexWord != null) {
                    List<SynonymSynset> synsetsInSameLevel = getSynsetsInSameLevel(indexWord, inputPos);
                    int i = 0;
                    for (SynonymSynset synset : synsetsInSameLevel) {
                        if (i++ > limit) {
                            break;
                        }
                        synonyms.add(synset); // add synonyms
                    }

                    if (synonyms.size() < limit) { //add upto the limit
                        for (SynonymSynset simnym : getSynsetsOfWordsInSameLevel(indexWord, inputPos)) {
                            if (synonyms.size() >= limit) {
                                break;
                            }
                            synonyms.add(simnym);
                        }
                    }

                    if (synonyms.size() < limit) { //get hypernyms upto remaining limit
                        for (SynonymSynset hypernym : getHyperSynsets(indexWord, inputPos)) {
                            if (synonyms.size() >= limit) {
                                break;
                            }
                            synonyms.add(hypernym);
                        }
                    }
                    if (synonyms.size() < limit) { //get siblings
                        for (SynonymSynset cousins : getSiblingSynsets(indexWord, inputPos)) {
                            if (synonyms.size() >= limit) {
                                break;
                            }
                            synonyms.add(cousins);
                        }
                    }
                }
            } catch (JWNLException e) {
                e.printStackTrace();
            }
        }
        List<SynonymSynset> synonymSynsets = new ArrayList<>(synonyms);
        synonymSynsets.sort(new SynonymSynsetComparatorByType());
        return synonymSynsets;
    }

    public SynonymSynset getOneSynsetForTheWord(String word, BotPOS inputPos, Link linkType) {
        IndexWord indexWord = null;
        try {
            indexWord = wordInWordNet(inputPos, word);
        } catch (JWNLException e) {
            return null;
        }
        SynonymSynset s = null;
        if (indexWord != null) {
            List<Synset> synsets = indexWord.getSenses();
            synsets.sort(new SynsetComparatorByOffset());
            Synset synset = synsets.get(0); // Get the synset with the lowest offset. This will be indexed as well as searched
            s = new SynonymSynset();
            s.setWord("" + synset.getOffset());
            s.setLemma("" + synset.getOffset());
            s.setSynset(synset);
            s.setLinkType(linkType);
            s.setPos(inputPos);
        }
        return s;
    }

    private List<SynonymSynset> getSynsetsInSameLevel(IndexWord word, BotPOS inputPos) {
        Set<SynonymSynset> synsets = new HashSet<>();
        for (Synset synset : word.getSenses()) {
            SynonymSynset s = new SynonymSynset();
            s.setWord("" + synset.getOffset());
            s.setLemma("" + synset.getOffset());
            s.setSynset(synset);
            s.setUseCount(synset.getWords().get(synset.indexOfWord(word.getLemma())).getUseCount());
            s.setLinkType(Link.syns);
            s.setPos(inputPos);
            synsets.add(s);
        }
        List<SynonymSynset> synonymSynsets = new ArrayList<>(synsets);
        synonymSynsets.sort(new SynonymSynsetComparatorByUseCount());
        return synonymSynsets.subList(0, Math.min(6, synonymSynsets.size())); //Taking only up to 5 synsets
    }

    private List<SynonymSynset> getSynsetsOfWordsInSameLevel(IndexWord word, BotPOS inputPos) throws JWNLException {
        //e.g. risk, and danger. Although they appear in same synset,
        // danger has multiple synsets and the first one does not match risk's
        Set<String> uniqueWords = new HashSet<>();
        Set<SynonymSynset> uniqueSynsets = new HashSet<>();

        for (SynonymSynset synset : getSynsetsInSameLevel(word, inputPos)) {
            for (Word x : synset.getSynset().getWords()) {
                uniqueWords.add(x.getLemma());
            }
        }
        for (String aWord : uniqueWords) {//Get unique synsets for all the above words (some will not be in same tree)
            SynonymSynset synset = getOneSynsetForTheWord(aWord, inputPos, Link.sim); // Get the synset with the lowest offset. This will be indexed as well as searched
            if (synset != null) {
                uniqueSynsets.add(synset);
            }
        }
        return new ArrayList<>(uniqueSynsets);
    }

    private List<SynonymSynset> getHyperSynsets(IndexWord word, BotPOS inputPos) throws JWNLException {
        Set<SynonymSynset> uniqueSynsets = new HashSet<>();
        Set<String> uniqueWords = new HashSet<>();

        PointerTargetNodeList pointerTargetNodes;
        for (SynonymSynset synset : getSynsetsInSameLevel(word, inputPos)) {
            if (inputPos == BotPOS.ADJECTIVE || inputPos == BotPOS.ADVERB) {
                pointerTargetNodes = PointerUtils.getSynonyms(synset.getSynset());
            } else {
                pointerTargetNodes = PointerUtils.getDirectHypernyms(synset.getSynset());
            }
            for (PointerTargetNode ptr : pointerTargetNodes) {
                for (Word x : ptr.getSynset().getWords()) {
                    uniqueWords.add(x.getLemma());
                }
            }
            for (String aWord : uniqueWords) {//Get unique synsets for all the above words (some will not be in same tree)
                SynonymSynset topSynset = getOneSynsetForTheWord(aWord, inputPos, Link.hype); // Get the synset with the lowest offset. This will be indexed as well as searched
                if (synset != null) {
                    uniqueSynsets.add(topSynset);
                }
            }
        }
        return new ArrayList<>(uniqueSynsets);
    }

    private List<SynonymSynset> getSiblingSynsets(IndexWord word, BotPOS inputPos) throws JWNLException {
        List<SynonymSynset> currentLevelSynsets = getSynsetsInSameLevel(word, inputPos);
        Set<SynonymSynset> uniqueSynsets = new HashSet<>();
        Set<String> uniqueWords = new HashSet<>();

        if (inputPos != BotPOS.ADJECTIVE && inputPos != BotPOS.ADVERB) {
            for (SynonymSynset synset : getHyperSynsets(word, inputPos)) {
                for (PointerTargetNode ptr : PointerUtils.getDirectHyponyms(synset.getSynset())) {
                    for (Word x : ptr.getSynset().getWords()) {
                        uniqueWords.add(x.getLemma());
                    }
                }
            }
            for (String aWord : uniqueWords) {//Get unique synsets for all the above words (some will not be in same tree)
                SynonymSynset topSynset = getOneSynsetForTheWord(aWord, inputPos, Link.hypo); // Get the synset with the lowest offset. This will be indexed as well as searched
                if (topSynset != null) {
                    uniqueSynsets.add(topSynset);
                }
            }
        }
        return new ArrayList<>(uniqueSynsets);
}

    public BotWord getTopVerbForNounWithPreposition(String inputWord) {
        List<BotWord> topVerbList = getVerbForNounWithPreposition(inputWord);
        BotWord topVerb = null;
        double previousScore = Double.MAX_VALUE;
        for (BotWord verb : topVerbList) {
            double score = ed.score(inputWord, verb.getWord()); // Using Lucene's EditDistance to get closest verbs
            if (score < previousScore) {
                topVerb = verb;
                previousScore = score;
            }
        }
        return topVerb;
    }

    private List<BotWord> getVerbForNounWithPreposition(String inputWord) {
        Set<Synset> synsets = new HashSet<>();
        Set<Pointer> pointers = new HashSet<>();
        Set<Synset> targetSynsets = new HashSet<>();
        Set<Word> verbs = new HashSet<>();
        Set<BotWord> synverbs = new HashSet<>();
        IndexWord word = null;
        try {
            word = wordInWordNet(BotPOS.NOUN, inputWord);

        } catch (JWNLException e) {
            e.printStackTrace();
        }
        if (word != null) {
            synsets.addAll(word.getSenses());
            for (Synset synset : synsets) {
                pointers.addAll(synset.getPointers(PointerType.DERIVATION));
            }
            for (Pointer y : pointers) {
                try {
                    targetSynsets.add(y.getTargetSynset());
                } catch (JWNLException e) {
                    e.printStackTrace();
                }
            }
            for (Synset targetSynset : targetSynsets) {
                verbs.addAll(targetSynset.getWords());
            }
            for (Word verb : verbs) {
                if (verb.getPOS() == POS.VERB) {
                    BotWord synonym = new BotWord();
                    synonym.setWord(verb.getLemma());
                    synonym.setLemma(verb.getLemma());
                    synonym.setLinkType(Link.also);
                    synonym.setPos(BotPOS.VERB);
                    synverbs.add(synonym);
                }
            }
        }
        return new ArrayList(synverbs);
    }
}