package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;
import java.util.Set;

public class AnswerResponse extends BotResponse implements Serializable {
    private Set<Match> matches;
    private Answer topAnswer;

    public Set<Match> getMatches() {
        return matches;
    }

    public void setMatches(Set<Match> matches) {
        this.matches = matches;
    }

    public Answer getTopAnswer() {
        return topAnswer;
    }

    public void setTopAnswer(Answer answer) {
        this.topAnswer = answer;
    }

}
