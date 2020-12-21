package org.arjunkashyap.learningbot.common;

import edu.cmu.lti.jawjaw.pobj.Link;
import edu.stanford.nlp.util.EditDistance;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.data.list.PointerTargetNode;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.dictionary.Dictionary;
import org.arjunkashyap.learningbot.Entity.BotPOS;
import org.arjunkashyap.learningbot.Entity.Synonym;
import org.arjunkashyap.learningbot.Entity.SynonymSynset;
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
        List<Synonym> synonyms;
        Synonym synonym;
        WordnetUtils wordnetUtils = new WordnetUtils();
        synonyms = wordnetUtils.getVerbForNounWithPreposition("meaning");
        //synonyms = wordnetUtils.getWordsInSameLevel(dictionary.getIndexWord(POS.NOUN, "phone"), BotPOS.noun);
        // synonyms = wordnetUtils.getHypernyms(dictionary.getIndexWord(POS.NOUN, "phone"), BotPOS.noun);

        // synonyms = wordnetUtils.getSiblings(dictionary.getIndexWord(POS.NOUN, "phone"), BotPOS.noun);
        //synonyms = wordnetUtils.getSiblings(dictionary.getIndexWord(POS.VERB, "catch up"), BotPOS.noun);
        //synonyms = wordnetUtils.getTopSynonyms("phone", BotPOS.verb, 10);
        synonym = wordnetUtils.getTopVerbForNounWithPreposition("president");
        System.out.println(synonyms);
        System.out.println(wordnetUtils.getHyperSynsets(dictionary.getIndexWord(POS.NOUN, "Phone"), BotPOS.NOUN));
    }

    private POS convertBotPosToPos(BotPOS botPOS) {
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

    public List<Synonym> getTopSynonyms(String inputWord, BotPOS inputPos, int limit) {
        List<Synonym> synonyms = new ArrayList<>();
        POS pos = null;
        Synonym mainWord = new Synonym();
        mainWord.setWord(inputWord);
        mainWord.setLemma(inputWord);
        mainWord.setScore(100);
        mainWord.setLinkType(Link.also);
        mainWord.setPos(inputPos);
        synonyms.add(mainWord);

        if (limit > 1 && inputPos != null && inputPos != BotPOS.WH_QUESTION && inputPos != BotPOS.CARDINAL_NUMBER) {
            pos = convertBotPosToPos(inputPos);
            IndexWord indexWord = null;
            try {
                indexWord = dictionary.getIndexWord(pos, inputWord);
            if (indexWord != null) {
                synonyms.addAll(getWordsInSameLevel(indexWord, inputPos)); // add synonyms
                if (synonyms.size() < limit) { //get hypernyms upto remaining limit
                    for (Synonym hypernym : getHypernyms(indexWord, inputPos)) {
                        if (synonyms.size() >= limit) {
                            break;
                        }
                        synonyms.add(hypernym);
                    }
                }
                if (synonyms.size() < limit) { //get siblings
                    for (Synonym cousins : getSiblings(indexWord, inputPos)) {
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
        return synonyms;
    }


    public List<SynonymSynset> getTopSynsets(String inputWord, BotPOS inputPos, int limit) {
        List<SynonymSynset> synonyms = new ArrayList<>();
        POS pos = null;
        if (limit >= 1 && inputPos != null && inputPos != BotPOS.WH_QUESTION && inputPos != BotPOS.CARDINAL_NUMBER) {
            pos = convertBotPosToPos(inputPos);
            IndexWord indexWord = null;
            try {
                indexWord = dictionary.getIndexWord(pos, inputWord);
                if (indexWord != null) {
                    List<SynonymSynset> synsetsInSameLevel = getSynsetsInSameLevel(indexWord, inputPos);
                    int i = 0;
                    for (SynonymSynset synset : synsetsInSameLevel){
                        if (i++ > limit) {
                            break;
                        }
                        synonyms.add(synset); // add synonyms
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
        return synonyms;
    }

    public List<SynonymSynset> getSynsetsOffsetInSameLevel(String word, BotPOS inputPos) {
        IndexWord indexWord = null;
        List<SynonymSynset> synsetOffsets = new ArrayList<>();
        try {
            indexWord = dictionary.getIndexWord(convertBotPosToPos(inputPos), word);
        } catch (JWNLException e) {
            e.printStackTrace();
        }
        if (indexWord != null) {
            /*for (SynonymSynset synonymSynset : getSynsetsInSameLevel(indexWord, inputPos)) {
                synsetOffsets.add(synonymSynset.getLemma());
            }*/
            return getSynsetsInSameLevel(indexWord, inputPos);
        }
        return synsetOffsets;
    }

    private List<SynonymSynset> getSynsetsInSameLevel(IndexWord word, BotPOS inputPos) {
        List<SynonymSynset> synsets = new ArrayList<>();
        for (Synset synset : word.getSenses()) {
            SynonymSynset s = new SynonymSynset();
            s.setWord(""+synset.getOffset());
            s.setLemma(""+synset.getOffset());
            s.setSynset(synset);
            s.setLinkType(Link.syns);
            s.setPos(inputPos);
            synsets.add(s);
        }
        return synsets;
    }

    private List<SynonymSynset> getHyperSynsets(IndexWord word, BotPOS inputPos) throws JWNLException {
        List<SynonymSynset> synsets = new ArrayList<>();
        PointerTargetNodeList pointerTargetNodes;
        for (SynonymSynset synset: getSynsetsInSameLevel(word, inputPos)){
            pointerTargetNodes = PointerUtils.getDirectHypernyms(synset.getSynset());
            for (PointerTargetNode ptr : pointerTargetNodes) {
                SynonymSynset s = new SynonymSynset();
                s.setWord(""+ptr.getSynset().getOffset());
                s.setLemma(""+ptr.getSynset().getOffset());
                s.setSynset(ptr.getSynset());
                s.setLinkType(Link.hype);
                s.setPos(inputPos);
                synsets.add(s);
            }
        }
        return synsets;
    }

    private List<SynonymSynset> getSiblingSynsets(IndexWord word, BotPOS inputPos) throws JWNLException { //TODO: Fixme
        List<SynonymSynset> currentLevelSynsets = getSynsetsInSameLevel(word, inputPos);
        List<SynonymSynset> synsets = new ArrayList<>();
        for (SynonymSynset synset : getHyperSynsets(word, inputPos)) {
            for (PointerTargetNode ptr : PointerUtils.getDirectHyponyms(synset.getSynset())) {
                if (!currentLevelSynsets.contains(ptr.getSynset())) {
                    SynonymSynset s = new SynonymSynset();
                    s.setWord(""+ptr.getSynset().getOffset());
                    s.setLemma(""+ptr.getSynset().getOffset());
                    s.setSynset(ptr.getSynset());
                    s.setPos(inputPos);
                    s.setLinkType(Link.hypo);
                    synsets.add(s);
                }
            }
        }
        return synsets;
    }

    private Set<Synonym> getWordsInSameLevel(IndexWord word, BotPOS pos) {
        Set<Synonym> synonyms = new HashSet<>();
        for (Synset sense : word.getSenses()) {
            for (Word syn : sense.getWords()) {
                Synonym synonym = new Synonym();
                synonym.setWord(syn.getLemma());
                synonym.setLemma(syn.getLemma());
                synonym.setLinkType(Link.syns);
                synonym.setPos(pos);
                synonym.setScore(100);
                synonyms.add(synonym);
            }
        }
        return synonyms;
    }

    private Set<Synonym> getHypernyms(IndexWord word, BotPOS pos) throws JWNLException {
        PointerTargetNodeList pointerTargetNodes;
        Set<Synonym> hypernyms = new HashSet<>();
        for (Synset synset : word.getSenses()) {
            pointerTargetNodes = PointerUtils.getDirectHypernyms(synset);
            for (PointerTargetNode ptr : pointerTargetNodes) {
                for (Word hypernym : ptr.getSynset().getWords()) {
                    Synonym synonym = new Synonym();
                    synonym.setWord(hypernym.getLemma());
                    synonym.setLemma(hypernym.getLemma());
                    synonym.setLinkType(Link.hype);
                    synonym.setPos(pos);
                    synonym.setScore(100);
                    hypernyms.add(synonym);
                }
            }
        }
        return hypernyms;
    }

    private Set<Synonym> getSiblings(IndexWord word, BotPOS pos) throws JWNLException {
        Set<Synset> currentLevelSynsets = new HashSet<>();
        Set<PointerTargetNode> hypernymPointerNodes = new HashSet<>();
        Set<PointerTargetNode> hyponymPointerNodes = new HashSet<>();
        Set<Synonym> hypernyms = new HashSet<>();
        for (Synset synset : word.getSenses()) {
            currentLevelSynsets.add(synset);
            hypernymPointerNodes.addAll(PointerUtils.getDirectHypernyms(synset));
        }
        for (PointerTargetNode ptr : hypernymPointerNodes) {
            hyponymPointerNodes.addAll(PointerUtils.getDirectHyponyms(ptr.getSynset()));
        }
        for (PointerTargetNode hyponymPointerNode : hyponymPointerNodes) {
            if (!currentLevelSynsets.contains(hyponymPointerNode.getSynset())) {
                for (Word hypernym : hyponymPointerNode.getSynset().getWords()) {
                    Synonym synonym = new Synonym();
                    synonym.setWord(hypernym.getLemma());
                    synonym.setLemma(hypernym.getLemma());
                    synonym.setLinkType(Link.hypo);
                    synonym.setPos(pos);
                    synonym.setScore(100);
                    hypernyms.add(synonym);
                }
            }
        }
        return hypernyms;
    }

    public Synonym getTopVerbForNounWithPreposition(String inputWord){
        List<Synonym> topVerbList = getVerbForNounWithPreposition(inputWord);
        Synonym topVerb = null;
        double previousScore = Double.MAX_VALUE;
        for (Synonym verb : topVerbList) {
            double score = ed.score(inputWord, verb.getWord()); // Using Lucene's EditDistance to get closest verbs
            if (score < previousScore) {
                topVerb = verb;
                previousScore = score;
            }
        }
        return topVerb;
    }

    private List<Synonym> getVerbForNounWithPreposition(String inputWord){
        Set<Synset> synsets = new HashSet<>();
        Set<Pointer> pointers = new HashSet<>();
        Set<Synset> targetSynsets = new HashSet<>();
        Set<Word> verbs = new HashSet<>();
        Set<Synonym> synverbs = new HashSet<>();
        IndexWord word = null;
        try {
            word = dictionary.getIndexWord(POS.NOUN, inputWord);
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
                    Synonym synonym = new Synonym();
                    synonym.setWord(verb.getLemma());
                    synonym.setLemma(verb.getLemma());
                    synonym.setLinkType(Link.also);
                    synonym.setPos(BotPOS.VERB);
                    synonym.setScore(100);
                    synverbs.add(synonym);
                }
            }
        }
        ArrayList<Synonym> verbList = new ArrayList<>();
        verbList.addAll(synverbs);
        return verbList;
    }
}