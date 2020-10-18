package com.example.demo.process;

import com.example.demo.Entity.Answer;
import com.example.demo.Entity.Match;
import com.example.demo.Entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnsweringMachine {
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private QuestionIndexer questionIndexer;
    private final String SQL = "select q.question, a.answer_id, answer " +
            "from question q, answer a, question_answer_relation r " +
             "where q.question_id = r.question_id " +
            "and r.answer_id=a.answer_id " +
            "and q.question_id = ?";

    public List<Match> getAnswer(Question inputQuestion) {
        List<Match> possibleMatches;
        if (inputQuestion.getIsQuestion()) {
            possibleMatches = questionIndexer.search(inputQuestion.getQuestionString());
            for (Match aMatch : possibleMatches) {
                System.out.println("QID: " + aMatch.getQuestion().getQuestionId() + " score: " + aMatch.getScore());
                if (aMatch.getScore() >= 0) {//TODO: check proper score
                    jtm.query(SQL, new Object[]{aMatch.getQuestion().getQuestionId()}, new RowCallbackHandler() {
                        @Override
                        public void processRow(ResultSet rs) throws SQLException {
                            aMatch.getQuestion().setQuestionString(rs.getString("question"));
                            Answer answer = new Answer();
                            answer.setAnswerId(rs.getInt("answer_id"));
                            answer.setAnswerString(rs.getString("answer"));
                            aMatch.setAnswer(answer);
                        }
                    });
                }
            }
            if (possibleMatches.size() == 0) {
                Question question = new Question();
                question.setQuestionString("Unknown");
                question.setQuestionId(-1);
                question.setIsQuestion(true);
                Answer answer = new Answer();
                answer.setAnswerId(-1);
                answer.setAnswerString("Sorry, I don't have an answer");
                Match match = new Match();
                match.setQuestion(question);
                match.setAnswer(answer);
                match.setScore(0.0f);
                possibleMatches.add(match);
            }
        } else {
            possibleMatches = new ArrayList<>();
            Question question = new Question();
            question.setQuestionString("Not a question");
            question.setQuestionId(-2);
            Answer answer = new Answer();
            answer.setAnswerId(-1);
            answer.setAnswerString("Ok");
            Match match = new Match();
            match.setQuestion(question);
            match.setAnswer(answer);
            match.setScore(0.0f);
            possibleMatches.add(match);
        }
        return possibleMatches;
    }
}