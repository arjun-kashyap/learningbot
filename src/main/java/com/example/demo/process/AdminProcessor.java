package com.example.demo.process;

import com.example.demo.Entity.AdminQuestionAnswerRelation;
import com.example.demo.Entity.Answer;
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
    private KnowledgeProcessor knowledgeProcessor;

    public void reIndex() {
        knowledgeProcessor.indexAllQuestions();
    }

    public List<AdminQuestionAnswerRelation> dumpQuestions() {
        List<AdminQuestionAnswerRelation> questionList = jtm.query("select * from question q, QUESTION_ANSWER_RELATION r, answer a " +
                "where q.question_id = r.question_id " +
                "and r.answer_id=a.answer_id", new RowMapper<AdminQuestionAnswerRelation>() {
            @Override
            public AdminQuestionAnswerRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
                AdminQuestionAnswerRelation r = new AdminQuestionAnswerRelation();
                Question question = new Question();
                question.setQuestionId(rs.getInt("question_id"));
                question.setQuestionString(rs.getString("question"));
                question.setMaxPossibleSearcherScore(rs.getFloat("max_possible_searcher_score"));
                r.setQuestion(question);
                Answer answer = new Answer();
                answer.setAnswerId(rs.getInt("answer_id"));
                answer.setAnswerString(rs.getString("answer"));
                r.setAnswer(answer);
                r.setManual(rs.getBoolean("manual"));
                r.setVotes(rs.getInt("votes"));
                return r;
            }
        });
        return questionList;
    }
}
