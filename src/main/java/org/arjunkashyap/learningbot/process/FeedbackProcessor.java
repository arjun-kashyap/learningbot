package org.arjunkashyap.learningbot.process;

import net.sf.extjwnl.JWNLException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.arjunkashyap.learningbot.Entity.Answer;
import org.arjunkashyap.learningbot.Entity.BotWord;
import org.arjunkashyap.learningbot.Entity.Match;
import org.arjunkashyap.learningbot.Entity.Question;
import org.arjunkashyap.learningbot.common.SentenceAnalyzer;
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
    @Autowired
    SentenceAnalyzer sentenceAnalyzer;

    public void incrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        try {
            int[] count = new int[]{1};
            List<BotWord> botWords = null;
            if (match.getSearcherScore() < 1) {
                botWords = sentenceAnalyzer.getPosElements(userQuestion.getQuestionString());
                String query = knowledgeProcessor.formQueryFromWords(botWords, true);
                System.out.println("qry:" + query);
                jtm.query("SELECT count(*) as counts FROM question WHERE question_words = ?", new Object[]{query}, new RowMapper<int[]>() {
                            @Override
                            public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                                count[0] = rs.getInt("counts");
                                return count;
                            }
                        }
                );
                System.out.println("cnt:" + count[0]);
            }
            if (count[0] == 0) {
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
                System.out.println("Adding question to database. " + questionID[0]);
                jtm.update(
                        "INSERT INTO QUESTION (question_id, question, max_possible_score_main, max_possible_score_synsets, create_date) values (?, ?, 100, 100, CURRENT_TIMESTAMP())",
                        userQuestion.getQuestionId(), userQuestion.getQuestionString()
                );
                jtm.update(
                        "INSERT INTO QUESTION_ANSWER_RELATION (question_id, answer_id, manual, votes, create_date) values (?, ?, false, ?, CURRENT_TIMESTAMP())",
                        userQuestion.getQuestionId(), match.getAnswer().getAnswerId(), Math.ceil(match.getWeightedFinalScore() * 10)
                );
                knowledgeProcessor.indexQuestion(userQuestion);
            } else {
                System.out.println("Increment QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
                jtm.update("update question_answer_relation set votes = votes+1 where question_id = ? and answer_id = ?",
                        question.getQuestionId(), answer.getAnswerId());
                //System.out.println("Question already exists in the database");
            }
        } catch (JWNLException | IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void decrementVote(Match match, Question userQuestion) {
        Question question = match.getQuestion();
        Answer answer = match.getAnswer();
        System.out.println("Decrement QuestionID: " + question.getQuestionId() + " & AnswerID: " + answer.getAnswerId());
        int[] votes = new int[]{1};
        jtm.query("SELECT votes FROM question_answer_relation WHERE question_id = ? and answer_id = ?", new Object[]{question.getQuestionId(), answer.getAnswerId()}, new RowMapper<int[]>() {
                    @Override
                    public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                        votes[0] = rs.getInt("votes");
                        return votes;
                    }
                }
        );
        System.out.println("Vote count:"+votes[0]);
        if (votes[0] > 0) {
            jtm.update("update question_answer_relation set votes = votes-1 where question_id = ? and answer_id = ?",
                    question.getQuestionId(), answer.getAnswerId());
        }
    }

    public void addQuestionToReviewList(Question userQuestion, String reviewType) {
        System.out.println("Problem Question: " + userQuestion.getCleanedQuestionString());
        int[] question_id = new int[]{0};
        jtm.query("SELECT nvl(id,0) question_id FROM unanswered_question WHERE question_string = ?", new Object[]{userQuestion.getCleanedQuestionString()}, new RowMapper<int[]>() {
                    @Override
                    public int[] mapRow(ResultSet rs, int rowNum) throws SQLException {
                        question_id[0] = rs.getInt("question_id");
                        return question_id;
                    }
                }
        );
        System.out.println("Question Id:"+question_id[0]);
        if (question_id[0] == 0) {
            jtm.update("insert into unanswered_question(question_string, problem_type, times_asked, create_date) values (?, ?, 1, CURRENT_TIMESTAMP())",
                    userQuestion.getCleanedQuestionString(), reviewType);
        }
        else {
            jtm.update("update unanswered_question set times_asked = times_asked+1 where id = ?", question_id[0]);
        }
    }
}
