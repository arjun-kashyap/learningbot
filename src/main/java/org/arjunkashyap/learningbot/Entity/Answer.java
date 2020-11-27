package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;

public class Answer implements Serializable {
    private int answerId;
    private String answerString;

    public int getAnswerId() {
        return answerId;
    }

    public void setAnswerId(int answerId) {
        this.answerId = answerId;
    }

    public String getAnswerString() {
        return answerString;
    }

    public void setAnswerString(String answerString) {
        this.answerString = answerString;
    }

    @Override
    public int hashCode() {
        return answerId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        Answer otherAnswer = (Answer) obj;
        return answerId == otherAnswer.answerId;
    }

    @Override
    public String toString() {
        return "{" +
                "answerId=" + answerId +
                ", answerString='" + answerString + '\'' +
                '}';
    }
}
