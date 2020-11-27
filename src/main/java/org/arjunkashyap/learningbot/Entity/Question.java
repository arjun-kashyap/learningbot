package org.arjunkashyap.learningbot.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Question implements Serializable {
    private int questionId;
    private String questionString;
    private float maxPossibleSearcherScore;
    @JsonIgnore
    private boolean isQuestion;
    private String noun;
    private String verb;
    @JsonIgnore
    private String tree;

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionString() {
        return questionString;
    }

    public void setQuestionString(String questionString) {
        this.questionString = questionString;
    }

    public boolean getIsQuestion() {
        return isQuestion;
    }

    public void setIsQuestion(boolean question) {
        isQuestion = question;
    }

    public String getNoun() {
        return noun;
    }

    public void setNoun(String noun) {
        this.noun = noun;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public float getMaxPossibleSearcherScore() {
        return maxPossibleSearcherScore;
    }

    public void setMaxPossibleSearcherScore(float maxPossibleSearcherScore) {
        this.maxPossibleSearcherScore = maxPossibleSearcherScore;
    }

    @Override
    public String toString() {
        return "Question{" +
                "questionId=" + questionId +
                ", questionString='" + questionString + '\'' +
                ", isQuestion=" + isQuestion +
                ", noun='" + noun + '\'' +
                ", verb='" + verb +
                '}';
    }
}
