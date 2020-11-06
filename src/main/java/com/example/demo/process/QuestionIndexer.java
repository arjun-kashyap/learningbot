package com.example.demo.process;

import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.Synonym;
import com.example.demo.Entity.WordClassification;
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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class QuestionIndexer implements InitializingBean {
    @Autowired
    private JdbcTemplate jtm;
    private IndexSearcher searcher;
    private StandardAnalyzer analyzer = new StandardAnalyzer();
    private Directory index;
    IndexReader reader;
    @Autowired
    private QuestionProcessor questionProcessor;

    static ILexicalDatabase db = new NictWordNet();
    static RelatednessCalculator rc = new WuPalmer(db);
    static {
        WS4JConfiguration.getInstance().setMFS(true);
    }


    public List<Match> search(String question) {
        List<Match> response = new ArrayList<>();
        try {
            int hitsPerPage = 10;
            int totalHitsThreshold = 10;
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
            List<WordClassification> wordClassifications = questionProcessor.getPosElements(question);
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            for (WordClassification word: wordClassifications) {
                if (word.getPos().startsWith("NN")){
                    nouns.append(" ").append(word.getLemma());
                }
                else if (word.getPos().startsWith("VB")) {
                    verbs.append(" ").append(word.getLemma());
                }
            }

            Query q = new QueryParser("questionString", analyzer).parse("nouns:"+nouns+" verbs:"+verbs);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Match match = new Match();
                match.setScore(hits[i].score);
                match.setQuestion(new Question());
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                System.out.println((i + 1) + ". " + d.get("questionId") + "\t" + d.get("question") + "\t" + hits[i].score);
                match.getQuestion().setQuestionId((Integer.parseInt(d.get("questionId"))));
                match.getQuestion().setIsQuestion(true);
                response.add(match);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public void afterPropertiesSet() {
        init();
    }

    public void init() {
        indexQuestions();
        createSearcher();
    }

    public void indexQuestions() {
        try {
            index = new MMapDirectory(Files.createTempDirectory("lucene").toAbsolutePath());
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter w = new IndexWriter(index, config);
            String sql = "select question_id, question from question";
            List<Question> questionList = jtm.query(sql, new RowMapper<Question>() {
                @Override
                public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Question question = new Question();
                    question.setQuestionId(rs.getInt("question_id"));
                    question.setQuestionString(rs.getString("question"));
                    return question;
                }
            });
            for (Question question : questionList) {
                //addDoc(w, question.getQuestionString(), question.getQuestionId()); ARJUN
                List<WordClassification> wordClassifications = questionProcessor.getPosElements(question.getQuestionString());
                addDoc(w, question.getQuestionId(), wordClassifications);
            }
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private IndexReader reOpenReader() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            reader = DirectoryReader.open(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reader;
    }

    private void createSearcher() {
        searcher = new IndexSearcher(reOpenReader());
    }

    //private static void addDoc(IndexWriter w, String questionString, int questionId) throws IOException {//ARJUN
    private void addDoc(IndexWriter w, int questionId, List<WordClassification> wordClassifications) throws IOException {
        Document doc = new Document();
        List<Set<Synonym>> list = new ArrayList<>();
        //TODO: stop words
        for (WordClassification word: wordClassifications) {
            Set<Synonym> synonymsForCurrentWord;
            POS pos = null;
            if (word.getPos().startsWith("NN"))
                pos = POS.n;
            else if (word.getPos().startsWith("VB"))
                pos = POS.v;
            if (pos != null) {
                System.out.println("Inside pos");
                list.add(getTopSynonyms(word.getLemma(), pos));
            }
        }

        List<List<Synonym>> allCombinationsForQuestion = getAllCombinations(list);

        for (List<Synonym> combination : allCombinationsForQuestion) {
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            for (Synonym synonym: combination) {
                if (synonym.getPos() == POS.n){
                    nouns.append(" "+synonym.getWord()); // nouns = nouns + " " + syniym.getWord();
                }
                else if (synonym.getPos() == POS.v) {
                    verbs.append(" "+synonym.getWord());
                }
            }

            doc.add(new TextField("nouns", nouns.toString(), Field.Store.NO));
            doc.add(new TextField("verbs", verbs.toString(), Field.Store.NO));
            doc.add(new StoredField("score", 100));
            doc.add(new StoredField("questionId", questionId));
            w.addDocument(doc);
        }

        //doc.add(new TextField("questionString", questionString, Field.Store.YES));
        //Add the main question's nouns and verbs  to the index
        //Add the nouns and verbs variations to the index

    }
    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */

    private static Set<Synonym> getTopSynonyms(String inputWord, POS inputPos) {
        List<Word> words = WordDAO.findWordsByLemmaAndPos(inputWord, inputPos);
        Map<Synonym, Integer> synonymsMap = new HashMap<>();
        int score;
        //Add the input word itself with top score
        Synonym synonym = new Synonym();
        synonym.setWord(inputWord);
        synonym.setPos(inputPos);
        synonym.setScore(100);
        synonym.setLinkType(Link.also);
        synonymsMap.put(synonym, 100);
        int count =1; //TODO: get only top synonyms
        for (Word word : words) {
            List<Sense> sensesOfInputWord = SenseDAO.findSensesByWordid(word.getWordid());
            for (Sense senseOfInputWord : sensesOfInputWord) {
/*
                if (count > 900)
                    break;
*/
                String synsetId = senseOfInputWord.getSynset();
                //List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynset(synsetId);
                List<Synlink> synlinks = SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hype);
                synlinks.addAll(SynlinkDAO.findSynlinksBySynsetAndLink(synsetId, Link.hypo));
                for (Synlink synlink : synlinks) {
/*
                    if (count > 900)
                        break;
*/
                    List<Sense> sensesForFoundSynset = SenseDAO.findSensesBySynsetAndLang(synlink.getSynset2(), Lang.eng);
                    for (Sense senseOfFoundSynset : sensesForFoundSynset) {
/*
                        if (count > 900)
                            break;
*/
                        Word foundWord = WordDAO.findWordByWordid(senseOfFoundSynset.getWordid());
                        if (foundWord.getPos() == inputPos) {
                            Relatedness s = rc.calcRelatednessOfSynset(new Concept(synsetId, inputPos), new Concept(synlink.getSynset2(), inputPos));
                            score = (int) Math.round(s.getScore() * 100);
                            synonym = new Synonym();
                            synonym.setWord(foundWord.getLemma());
                            synonym.setScore(score);
                            synonym.setPos(inputPos);
                            synonym.setLinkType(synlink.getLink());
                            Integer existingSynonymScore = synonymsMap.get(synonym);
                            if (existingSynonymScore == null || existingSynonymScore < score) {
                                synonymsMap.put(synonym, score);
                                count++;
                            }
                        }
                    }
                }
            }
        }
        Set<Synonym> synSet = new TreeSet<>(synonymsMap.keySet()).descendingSet(); //TODO: take only top ones. How?

        return synSet;
    }

    private List<List<Synonym>> getAllCombinations(List<Set<Synonym>> listOfSetOfSynonyms) {//TODO add score by weighting
        System.out.println(listOfSetOfSynonyms);
        List<List<Synonym>> allCombinations = new ArrayList<>();
        if (listOfSetOfSynonyms.size()==1) {
            for (Synonym synonym: listOfSetOfSynonyms.get(0)) {
                List<Synonym> y = new ArrayList<Synonym>();
                y.add(synonym);
                allCombinations.add(y);
            }
        }
        else {
            Set<Synonym> synonymsOfFirstWord = listOfSetOfSynonyms.get(0);
            List<List<Synonym>> listOfSynOfRestOfWords = getAllCombinations(listOfSetOfSynonyms.subList(1,listOfSetOfSynonyms.size()));
            for (List<Synonym> previousCombinations: listOfSynOfRestOfWords) {
                for (Synonym synonym: synonymsOfFirstWord) {
                    List<Synonym> combination = new ArrayList<Synonym>();
                    combination.add(synonym);
                    combination.addAll(previousCombinations);
                    allCombinations.add(combination);
                }
            }
        }
        return allCombinations;
    }
}

//https://stackoverflow.com/questions/21965778/sorting-search-result-in-lucene-based-on-a-numeric-field

//https://lucene.apache.org/core/2_9_4/queryparsersyntax.html