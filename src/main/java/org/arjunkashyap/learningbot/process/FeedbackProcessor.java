package org.arjunkashyap.learningbot.process;

import org.arjunkashyap.learningbot.Entity.Answer;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class FeedbackProcessor {//TODO: Processor of feedback
    @Autowired
    private JdbcTemplate jtm;

    public void incrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        if (match.getSearcherScore() > 2) {
            System.out.println("Increment QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
            jtm.update("update question_answer_relation set votes = votes+1 where question_id = ? and answer_id = ?",
                    question.getQuestionId(), answer.getAnswerId());
        }
        else {
            int[] questionID = new int[1];
                    jtm.query("SELECT QUESTION_SEQUENCE.NEXTVAL AS NEXT_QUESTION_ID", new RowMapper<int[]>() {
                        @Override
                        public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                            questionID[0] = rs.getInt("NEXT_QUESTION_ID");
                            return questionID;
                        }
                    }
            );

            System.out.println("Adding question to database. "+questionID[0]);
            jtm.update(
                    "INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (?, ?, 100, 100, CURRENT_TIMESTAMP())",
                    questionID[0], userQuestion.getQuestionString()
            );
            jtm.update(
                    "INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (?, ?, true, 1, CURRENT_TIMESTAMP())",
                    questionID[0], match.getAnswer().getAnswerId()
            );
        }
    }

    public void decrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        System.out.println("Decrement QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
        jtm.update("update question_answer_relation set votes = votes-1 where question_id = ? and answer_id = ?",
                question.getQuestionId(), answer.getAnswerId());
    }
}
