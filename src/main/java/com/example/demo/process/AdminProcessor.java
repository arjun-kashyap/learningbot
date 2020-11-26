package com.example.demo.process;

import com.example.demo.Entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class AdminProcessor {
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private IndexProcessor indexProcessor;

    public void reIndex() {
        indexProcessor.indexAllQuestions();
    }

    public List<Question> dumpQuestions() {
        List<Question> questionList = jtm.query("select * from question", new RowMapper<Question>() {
            @Override
            public Question mapRow(ResultSet rs, int rowNum) throws SQLException {
                Question question = new Question();
                question.setQuestionId(rs.getInt("question_id"));
                question.setQuestionString(rs.getString("question"));
                question.setMaxPossibleSearcherScore(rs.getFloat("max_possible_searcher_score"));
                return question;
            }
        });
        for (Question q: questionList) {
            System.out.println("Question Max: "+q.getMaxPossibleSearcherScore());
        }
        return null;
    }
}
