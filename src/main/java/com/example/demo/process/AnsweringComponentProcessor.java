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
import java.util.*;

@Component
public class AnsweringComponentProcessor {//TODO: get only the best question match by score. Score the questions properly
    @Autowired
    private JdbcTemplate jtm;
    @Autowired
    private Indexer indexer;
    private final String SQL = "select q.question, a.answer_id, answer, votes, manual, max_possible_searcher_score " +
            "from question q, answer a, question_answer_relation r " +
             "where q.question_id = r.question_id " +
            "and r.answer_id=a.answer_id " +
            "and q.question_id = ?";

    public TreeSet<Match> getAnswer(Question inputQuestion) {
        List<Match> possibleMatches;
        if (inputQuestion.getIsQuestion()) {
            possibleMatches = indexer.search(inputQuestion.getQuestionString());
            for (Match match : possibleMatches) {
                //System.out.println("QID: " + match.getQuestion().getQuestionId() + " score: " + match.getSearcherScore());
                if (match.getSearcherScore() >= 0) {//TODO: check proper score
                    jtm.query(SQL, new Object[]{match.getQuestion().getQuestionId()}, new RowCallbackHandler() {
                        @Override
                        public void processRow(ResultSet rs) throws SQLException {
                            float voteScore;
                            match.getQuestion().setQuestionString(rs.getString("question"));
                            match.getQuestion().setMaxPossibleSearcherScore(rs.getFloat("max_possible_searcher_score"));
                            if (rs.getBoolean("manual"))
                                voteScore = 1;
                            else
                                voteScore = Math.max(rs.getFloat("votes"), 10)/10; //TODO: Make this a property/database value
                            match.setVoteScore(voteScore);
                            match.setSearcherScore(match.getSearcherScore()/match.getQuestion().getMaxPossibleSearcherScore());
                            Answer answer = new Answer();
                            answer.setAnswerId(rs.getInt("answer_id"));
                            answer.setAnswerString(rs.getString("answer"));
                            match.setAnswer(answer);
                            match.setWeightedFinalScore(match.getSearcherScore()*match.getSynonymScore()*match.getVoteScore()); //TODO: Logic?
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
                match.setSearcherScore(0.0f);
                match.setSynonymScore(0.0f);
                match.setVoteScore(0.0f);
                match.setWeightedFinalScore(0.0f);
                possibleMatches.add(match);
            }
        }
        else {
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
            match.setSearcherScore(0.0f);
            match.setSynonymScore(0.0f);
            match.setVoteScore(0.0f);
            match.setWeightedFinalScore(0.0f);
            possibleMatches.add(match);
        }
        Collections.sort(possibleMatches, Comparator.comparingDouble(Match::getWeightedFinalScore).reversed());
        TreeSet<Match> topMatches = new TreeSet<>();
        for (Match match: possibleMatches) {
            topMatches.add(match);
        }
        return topMatches;
    }
}