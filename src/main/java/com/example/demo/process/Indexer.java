package com.example.demo.process;

import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.Synonym;
import com.example.demo.Entity.WordClassification;
import com.example.demo.common.SentenceAnalyzer;
import edu.cmu.lti.jawjaw.db.SenseDAO;
import edu.cmu.lti.jawjaw.db.SynlinkDAO;
import edu.cmu.lti.jawjaw.db.WordDAO;
import edu.cmu.lti.jawjaw.pobj.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class Indexer {
    @Autowired
    private JdbcTemplate jtm;
    private IndexSearcher searcher;
    private final StandardAnalyzer analyzer = new StandardAnalyzer();
    private Directory index;
    IndexReader reader;
    @Autowired
    private SentenceAnalyzer sentenceAnalyzer;

    public List<Match> search(String question) {
        List<Match> response = new ArrayList<>();
        try {
            int exactCount;
            int synonymCount;
            int hypernymCount;
            int hyponymCount;
            double synscore;

            int hitsPerPage = 1000;
            int totalHitsThreshold = 1000;
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
            List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question);
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            for (WordClassification word : wordClassifications) {
                if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                    nouns.append(" ").append(word.getLemma() + "~"); // ~ is appended for Fuzzy query
                } else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                    verbs.append(" ").append(word.getLemma() + "~");
                }
            }
            StringBuilder query = new StringBuilder();
            if (nouns.length() > 0) {
                query.append("+nouns:(" + nouns.toString().trim() + ")");
            }
            if (verbs.length() > 0) {
                query.append(" +verbs:(" + verbs.toString().trim() + ")");
            }
            //System.out.println("Query is:" + query.toString().trim());
            Query q = new QueryParser("", analyzer).parse(query.toString());
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            //System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Match match = new Match();
                match.setSearcherScore(hits[i].score);
                match.setQuestion(new Question());
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);

                //System.out.println((i + 1) + ". " + d.get("questionId") + "\t" + d.get("question") + "\t" + hits[i].score+ "\t" +
                //        d.get("exactCount")+ "\t" +d.get("synonymCount")+ "\t" +d.get("hypernymCount")+ "\t" +d.get("hyponymCount"));
                match.getQuestion().setQuestionId((Integer.parseInt(d.get("questionId"))));
                match.getQuestion().setIsQuestion(true);
                exactCount = Integer.parseInt(d.get("exactCount"));
                synonymCount = Integer.parseInt(d.get("synonymCount"));
                hypernymCount = Integer.parseInt(d.get("hypernymCount"));
                hyponymCount = Integer.parseInt(d.get("hyponymCount"));
                synscore = (exactCount*1+synonymCount*0.9+hypernymCount*0.8+hyponymCount*0.7) / (exactCount+synonymCount+hypernymCount+hyponymCount);
                match.setSynonymScore((float)synscore);
                match.setDebug(d.get("debug"));
                response.add(match);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return response;
    }

   /* @Override
    public void afterPropertiesSet() throws SQLException {
        init();
    }*/

    @PostConstruct
    public void init() {
        indexQuestions();
        createSearcher();
        populateMaxScoreForEachQuestion();
    }

    public void indexQuestions() {
        try {
            index = new MMapDirectory(Files.createTempDirectory("lucene").toAbsolutePath());
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter indexWriter = new IndexWriter(index, config);
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
                addQuestionToIndex(indexWriter, question);
            }
            indexWriter.close();
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

    public void populateMaxScoreForEachQuestion() {
        try {
            String sql = "select question_id, question from question";
            StringBuilder nouns;
            StringBuilder verbs;
            StringBuilder query;
            Query q;
            TopScoreDocCollector collector;
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
                List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question.getQuestionString());
                nouns = new StringBuilder();
                verbs = new StringBuilder();
                for (WordClassification word : wordClassifications) {
                    if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                        nouns.append(" ").append(word.getLemma() + "~"); // ~ is appended for Fuzzy query
                    } else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma()))) {
                        verbs.append(" ").append(word.getLemma() + "~");
                    }
                }
                query = new StringBuilder();
                if (nouns.length() > 0) {
                    query.append("+nouns:(" + nouns.toString().trim() + ")");
                }
                if (verbs.length() > 0) {
                    query.append(" +verbs:(" + verbs.toString().trim() + ")");
                }
                //System.out.println("Query is:" + query.toString().trim());
                q = new QueryParser("", analyzer).parse(query.toString());
                collector = TopScoreDocCollector.create(1, 1);
                searcher.search(q, collector);
                ScoreDoc[] hits = collector.topDocs().scoreDocs;
                question.setMaxPossibleSearcherScore(hits[0].score);
                jtm.update("update question set max_possible_searcher_score = ? where question_id = ?",
                        question.getMaxPossibleSearcherScore(), question.getQuestionId());
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private void addQuestionToIndex(IndexWriter indexWriter, Question question) throws IOException {
        Document doc;
        List<Set<Synonym>> list = new ArrayList<>();
        int exactCount;
        int synonymCount;
        int hypernymCount;
        int hyponymCount;

        List<WordClassification> wordClassifications = sentenceAnalyzer.getPosElements(question.getQuestionString());

        for (WordClassification word : wordClassifications) {
            POS pos = null;
            if (word.getPos().startsWith("NN") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())))
                pos = POS.n;
            else if (word.getPos().startsWith("VB") && !(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.getLemma())))
                pos = POS.v;
            if (pos != null) {
                StringBuilder nouns = new StringBuilder();
                StringBuilder verbs = new StringBuilder();

                list.add(getTopSynonyms(word.getLemma(), pos));
            }
        }

        List<List<Synonym>> allCombinationsForQuestion = getAllCombinations(list);

        for (List<Synonym> combination : allCombinationsForQuestion) {
            doc = new Document();
            StringBuilder nouns = new StringBuilder();
            StringBuilder verbs = new StringBuilder();
            exactCount = synonymCount = hypernymCount = hyponymCount = 0;
            for (Synonym synonym : combination) {
                switch (synonym.getLinkType()) {
                    case also:
                        exactCount++;
                        break;
                    case syns:
                        synonymCount++;
                        break;
                    case hype:
                        hypernymCount++;
                        break;
                    case hypo:
                        hyponymCount++;
                        break;
                }

                if (synonym.getPos() == POS.n) {
                    nouns.append(" " + synonym.getWord());
                } else if (synonym.getPos() == POS.v) {
                    verbs.append(" " + synonym.getWord());
                }
            }
            //System.out.println("Adding to Index. Nouns: "+ nouns+" verbs: "+verbs);
            doc.add(new TextField("nouns", nouns.toString(), Field.Store.NO));
            doc.add(new TextField("verbs", verbs.toString(), Field.Store.NO));
            doc.add(new StoredField("exactCount", exactCount));
            doc.add(new StoredField("synonymCount", synonymCount));
            doc.add(new StoredField("hypernymCount", hypernymCount));
            doc.add(new StoredField("hyponymCount", hyponymCount));
            doc.add(new StoredField("questionId", question.getQuestionId()));
            doc.add(new StoredField("debug", "nouns:"+nouns.toString()+" verbs: "+verbs.toString())); //Storing the field for analyzing
            indexWriter.addDocument(doc);
        }
    }

    private List<List<Synonym>> getAllCombinations(List<Set<Synonym>> listOfSetOfSynonyms) {//TODO add score by weighting
        //System.out.println("HERE: " + listOfSetOfSynonyms);
        List<List<Synonym>> allCombinations = new ArrayList<>();
        if (listOfSetOfSynonyms.size() == 1) {
            for (Synonym synonym : listOfSetOfSynonyms.get(0)) {
                List<Synonym> y = new ArrayList<Synonym>();
                y.add(synonym);
                allCombinations.add(y);
            }
        } else {
            Set<Synonym> synonymsOfFirstWord = listOfSetOfSynonyms.get(0);
            List<List<Synonym>> listOfSynOfRestOfWords = getAllCombinations(listOfSetOfSynonyms.subList(1, listOfSetOfSynonyms.size()));
            for (List<Synonym> previousCombinations : listOfSynOfRestOfWords) {
                for (Synonym synonym : synonymsOfFirstWord) {
                    List<Synonym> combination = new ArrayList<>();
                    combination.add(synonym);
                    combination.addAll(previousCombinations);
                    allCombinations.add(combination);
                }
            }
        }
        return allCombinations;
    }

    /* Input word may have many senses (for the same part of speech). For each sense, get the word IDs.
    Then, for each of those words, get linked synsets
    For each of the linked synsets, get the words and their senses
    Compare the similarity of the original word senses with these (same POS)
     */
    private static Set<Synonym> getTopSynonyms(String inputWord, POS inputPos) {
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


//https://stackoverflow.com/questions/21965778/sorting-search-result-in-lucene-based-on-a-numeric-field

//https://lucene.apache.org/core/2_9_4/queryparsersyntax.html