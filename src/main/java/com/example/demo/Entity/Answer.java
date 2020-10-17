package com.example.demo.Entity;

import java.io.Serializable;

public class Answer implements Serializable {
    private int id;
    private String answer;
    private int rank;

    public Answer() {
    }

    public Answer(int id, String answer) {
        this.id = id;
        this.answer = answer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public int hashCode() {
        return (int) id * answer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        Answer otherAnswer = (Answer) obj;
        return id == otherAnswer.id && answer.equals(otherAnswer.answer);
    }

    @Override
    public String toString() {
        return "id: " + id + ", answer: " + answer;
    }

}
