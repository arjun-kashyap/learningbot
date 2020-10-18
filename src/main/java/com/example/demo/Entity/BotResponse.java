package com.example.demo.Entity;

import java.util.List;

public class BotResponse {
    private Answer topAnswer;
    private Question questionDecomposed;
    private List<Match> matchList;

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

    public List<Match> getMatchList() {
        return matchList;
    }

    public void setMatchList(List<Match> matchList) {
        this.matchList = matchList;
    }
}
