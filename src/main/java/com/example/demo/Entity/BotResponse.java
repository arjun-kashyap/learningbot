package com.example.demo.Entity;

import java.util.List;

public class BotResponse {
    private Answer topAnswer;
    private Question questionDecomposed;
    private List<Answer> otherAnswersList;

    public Answer getTopAnswer() {
        return topAnswer;
    }

    public void setTopAnswer(Answer answer) {
        this.topAnswer = answer;
    }

    public Question getQuestionDecomposed() {
        return questionDecomposed;
    }

    public void setQuestionDecomposed(Question questionDecomposed) {
        this.questionDecomposed = questionDecomposed;
    }

    public List<Answer> getOtherAnswersList() {
        return otherAnswersList;
    }

    public void setOtherAnswersList(List<Answer> otherAnswersList) {
        this.otherAnswersList = otherAnswersList;
    }
}
