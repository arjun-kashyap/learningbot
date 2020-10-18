package com.example.demo.process;

import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class QuestionIndexer implements InitializingBean {
    @Autowired
    private JdbcTemplate jtm;
    private IndexSearcher searcher;
    private StandardAnalyzer analyzer = new StandardAnalyzer();
    private Directory index;

    public List<Match> search(String queryString) {
        List<Match> response = new ArrayList<>();
        try {
            int hitsPerPage = 10;
            int totalHitsThreshold = 10;
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, totalHitsThreshold);
            Query q = new QueryParser("questionString", analyzer).parse(queryString);
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
    public void afterPropertiesSet() throws Exception {
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
                addDoc(w, question.getQuestionString(), question.getQuestionId());
            }
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSearcher() {
        IndexReader reader;
        try {
            reader = DirectoryReader.open(index);
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addDoc(IndexWriter w, String questionString, int questionId) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("questionString", questionString, Field.Store.YES));
        doc.add(new StoredField("questionId", questionId));
        w.addDocument(doc);
    }

}

//https://stackoverflow.com/questions/21965778/sorting-search-result-in-lucene-based-on-a-numeric-field

//https://lucene.apache.org/core/2_9_4/queryparsersyntax.html