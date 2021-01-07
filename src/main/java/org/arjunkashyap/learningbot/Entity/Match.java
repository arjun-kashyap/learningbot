package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;
import java.util.Objects;

public class Match implements Comparable<Match>, Serializable {
    private Question question;
    private Answer answer;
    private float searcherScore;
    private float synonymScore;
    private float voteScore;
    private float weightedFinalScore;
    private String debug;

    public void setVoteScore(float voteScore) {
        this.voteScore = voteScore;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public float getSearcherScore() {
        return searcherScore;
    }

    public void setSearcherScore(float searcherScore) {
        this.searcherScore = searcherScore;
    }

    public float getSynonymScore() {
        return synonymScore;
    }

    public void setSynonymScore(float synonymScore) {
        this.synonymScore = synonymScore;
    }

    public float getVoteScore() {
        return voteScore;
    }

    public float getWeightedFinalScore() {
        return weightedFinalScore;
    }

    public void setWeightedFinalScore(float weightedFinalScore) {
        this.weightedFinalScore = weightedFinalScore;
    }

    public String getDebug() {
        return debug;
    }

    public void setDebug(String debug) {
        this.debug = debug;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return answer.equals(match.answer);
                //&& question.equals(match.question); Add this to see the other POS and questions that were matched. REMEMBER to update hashCode
    }

    @Override
    public int hashCode() {
        return Objects.hash(answer);
    }

    @Override
    public String toString() {
        return "Match{" +
                "answer=" + answer +
                ", question=" + question +
                ", searcherScore=" + searcherScore +
                ", synonymScore=" + synonymScore +
                ", voteScore=" + voteScore +
                ", weightedFinalScore=" + weightedFinalScore +
                ", debug='" + debug + '\'' +
                '}';
    }

    @Override
    public int compareTo(Match o) {
        return answer.equals(o.answer)?0:1;
    }
}