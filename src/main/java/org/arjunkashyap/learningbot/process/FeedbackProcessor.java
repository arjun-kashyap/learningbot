package org.arjunkashyap.learningbot.process;

import net.sf.extjwnl.JWNLException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.arjunkashyap.learningbot.Entity.Answer;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class FeedbackProcessor {
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private KnowledgeProcessor knowledgeProcessor;

    public void incrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        if (match.getSearcherScore() > 2) {//TODO: this is to force the else. Remove once validated.
            System.out.println("Increment QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
            jtm.update("update question_answer_relation set votes = votes+1 where question_id = ? and answer_id = ?",
                    question.getQuestionId(), answer.getAnswerId());
        }
        else {//TODO: Need to de-duplicate
            int[] questionID = new int[1];
                    jtm.query("SELECT QUESTION_SEQUENCE.NEXTVAL AS NEXT_QUESTION_ID", new RowMapper<int[]>() {
                        @Override
                        public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                            questionID[0] = rs.getInt("NEXT_QUESTION_ID");
                            return questionID;
                        }
                    }
            );
            userQuestion.setQuestionId(questionID[0]);
            System.out.println("Adding question to database. "+questionID[0]);
            jtm.update(
                    "INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (?, ?, 100, 100, CURRENT_TIMESTAMP())",
                    userQuestion.getQuestionId(), userQuestion.getQuestionString()
            );
            jtm.update(
                    "INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (?, ?, true, 1, CURRENT_TIMESTAMP())",
                    userQuestion.getQuestionId(), match.getAnswer().getAnswerId()
            );
            knowledgeProcessor.indexQuestion(userQuestion);
        }
    }

    public void decrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        System.out.println("Decrement QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
        jtm.update("update question_answer_relation set votes = votes-1 where question_id = ? and answer_id = ?",
                question.getQuestionId(), answer.getAnswerId());
    }
    public void addQuestionToReviewList(Question userQuestion) {
        System.out.println("Problem Question: " + userQuestion.getQuestionString());
        //TODO: jtm.update("update question_answer_relation set votes = votes-1 where question_id = ? and answer_id = ?",
              //  question.getQuestionId(), answer.getAnswerId());
    }
}
