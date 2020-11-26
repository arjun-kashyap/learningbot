package com.example.demo.process;

import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import com.example.demo.Entity.Synonym;
import com.example.demo.Entity.WordClassification;
import com.example.demo.common.SearchIndex;
import com.example.demo.common.SentenceAnalyzer;
import com.example.demo.common.Utilities;
import com.example.demo.common.WordNetUtilities;
import edu.cmu.lti.jawjaw.pobj.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class KnowledgeProcessor {
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private SentenceAnalyzer sentenceAnalyzer;
    @Autowired
    private SearchIndex searchIndex;

    public List<Match> search(String question) {
        List<Match> response = new ArrayList<>();
        try {
            int exactCount, synonymCount, hypernymCount, hyponymCount;
            int hitsPerPage, totalHitsThreshold;
            double synScore;

            hitsPerPage = totalHitsThreshold = 1000; //TODO: Move to properties/database
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
            Query q = new QueryParser("", searchIndex.getAnalyzer()).parse(query.toString());
            searchIndex.getIndexSearcher().search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            //System.out.println("Found " + hits.length + " hits.");
            for (int i = 0; i < hits.length; ++i) {
                Match match = new Match();
                match.setSearcherScore(hits[i].score);
                match.setQuestion(new Question());
                int docId = hits[i].doc;
                Document d = searchIndex.getIndexSearcher().doc(docId);

                //System.out.println((i + 1) + ". " + d.get("questionId") + "\t" + d.get("question") + "\t" + hits[i].score+ "\t" +
                //        d.get("exactCount")+ "\t" +d.get("synonymCount")+ "\t" +d.get("hypernymCount")+ "\t" +d.get("hyponymCount"));
                match.getQuestion().setQuestionId((Integer.parseInt(d.get("questionId"))));
                match.getQuestion().setIsQuestion(true);
                exactCount = Integer.parseInt(d.get("exactCount"));
                synonymCount = Integer.parseInt(d.get("synonymCount"));
                hypernymCount = Integer.parseInt(d.get("hypernymCount"));
                hyponymCount = Integer.parseInt(d.get("hyponymCount"));
                //TODO: Move weightage to properties/database and come up with a good number
                synScore = (exactCount*1+synonymCount*0.9+hypernymCount*0.8+hyponymCount*0.7) / (exactCount+synonymCount+hypernymCount+hyponymCount);
                match.setSynonymScore((float)synScore);
                match.setDebug(d.get("debug"));
                response.add(match);
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    @PostConstruct
    public void indexAllQuestions() {
        try {
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
            IndexWriter indexWriter = searchIndex.getIndexWriterForWrite();
            for (Question question : questionList) {
                addQuestionToIndex(indexWriter, question);
            }
            searchIndex.closeIndexWriter(indexWriter);

            StringBuilder nouns;
            StringBuilder verbs;
            StringBuilder query;
            Query q;
            TopScoreDocCollector collector;

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
                q = new QueryParser("", searchIndex.getAnalyzer()).parse(query.toString());
                collector = TopScoreDocCollector.create(1, 1);
                IndexSearcher searcher = searchIndex.getIndexSearcher();
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
        int exactCount, synonymCount, hypernymCount, hyponymCount;

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

                list.add(WordNetUtilities.getTopSynonyms(word.getLemma(), pos));
            }
        }

        List<List<Synonym>> allCombinationsForQuestion = Utilities.getAllCombinations(list);

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
}


//https://stackoverflow.com/questions/21965778/sorting-search-result-in-lucene-based-on-a-numeric-field

//https://lucene.apache.org/core/2_9_4/queryparsersyntax.html