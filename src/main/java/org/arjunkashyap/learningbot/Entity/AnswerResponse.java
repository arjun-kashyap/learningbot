package org.arjunkashyap.learningbot.Entity;

import java.util.Set;

public class AnswerResponse extends BotResponse{
    private Answer topAnswer;
    private Set<Match> matchList;

    public Answer getTopAnswer() {
        return topAnswer;
    }

    public void setTopAnswer(Answer answer) {
        this.topAnswer = answer;
    }

    public Set<Match> getMatchList() {
        return matchList;
    }

    public void setMatchList(Set<Match> matchList) {
        this.matchList = matchList;
    }

}
