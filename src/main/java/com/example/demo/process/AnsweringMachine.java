package com.example.demo.process;

import com.example.demo.Entity.Answer;
import com.example.demo.Entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnsweringMachine {
    @Autowired
    private JdbcTemplate jtm;

    public List<Answer> getAnswer (Question question) {
        List<Answer> answerList = null;
        String sql = "select a.answer_id id, answer " +
                "from question q, answer a, question_answer_relation r " +
                "where q.question_id = r.question_id " +
                "and r.answer_id=a.answer_id " +
                "and q.question=?";

        if (question.isQuestion()) {
            answerList = jtm.query(sql, new Object[]{question.getQuestion()}, new RowMapper<Answer>() {
                @Override
                public Answer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    Answer answer = new Answer();
                    answer.setId(rs.getInt("id"));
                    answer.setAnswer(rs.getString("answer"));
                    return answer;
                }
            });
            if (answerList.size() == 0) {
                answerList.add(new Answer(0, "Sorry, I don't have an answer"));
            }
        }
        else {
            answerList = new ArrayList<>();
            answerList.add(new Answer(0, "ok"));
        }
        return answerList;
    }
}