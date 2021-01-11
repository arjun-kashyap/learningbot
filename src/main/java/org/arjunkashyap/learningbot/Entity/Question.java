package org.arjunkashyap.learningbot.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

public class Question implements Serializable {
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int questionId;
    private String questionString;
    @JsonIgnore
    private float maxPossibleScoreForMainWords;
    @JsonIgnore
    private float maxPossibleScoreForSynsets;
    @JsonIgnore
    private boolean isQuestion;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String tree;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String parser;

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

    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }

    public float getMaxPossibleScoreForMainWords() {
        return maxPossibleScoreForMainWords;
    }

    public void setMaxPossibleScoreForMainWords(float maxPossibleScoreForMainWords) {
        this.maxPossibleScoreForMainWords = maxPossibleScoreForMainWords;
    }

    public float getMaxPossibleScoreForSynsets() {
        return maxPossibleScoreForSynsets;
    }

    public void setMaxPossibleScoreForSynsets(float maxPossibleScoreForSynsets) {
        this.maxPossibleScoreForSynsets = maxPossibleScoreForSynsets;
    }

    public String getParser() {
        return parser;
    }

    public void setParser(String parser) {
        this.parser = parser;
    }

    @Override
    public String toString() {
        return "Question{" +
                "questionId=" + questionId +
                ", questionString='" + questionString + '\'' +
                ", isQuestion=" + isQuestion +
                '}';
    }
}
