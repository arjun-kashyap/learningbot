package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;

public class AdminQuestionAnswerRelation implements Serializable {
    private Question question;
    private Answer answer;
    private boolean isManual;
    private int votes;

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

    public boolean isManual() {
        return isManual;
    }

    public void setManual(boolean manual) {
        isManual = manual;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    @Override
    public String toString() {
        return "AdminQuestionAnswerRelation{" +
                "question=" + question +
                ", answer=" + answer +
                ", isManual=" + isManual +
                ", votes=" + votes +
                '}';
    }
}
