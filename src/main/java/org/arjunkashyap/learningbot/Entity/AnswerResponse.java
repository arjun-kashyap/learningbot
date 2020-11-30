package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;
import java.util.List;

public class AnswerResponse extends BotResponse implements Serializable {
    private List<Match> matches;
    private Answer topAnswer;
    private boolean moreAnswers; //TODO: implement logic to enable button functionality

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public Answer getTopAnswer() {
        return topAnswer;
    }

    public void setTopAnswer(Answer answer) {
        this.topAnswer = answer;
    }
}
