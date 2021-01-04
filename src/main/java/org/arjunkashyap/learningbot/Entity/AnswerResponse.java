package org.arjunkashyap.learningbot.Entity;

import java.io.Serializable;
import java.util.List;

public class AnswerResponse extends BotResponse implements Serializable {
    private List<Match> matches;
    private int topMatchIndex;

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public int getTopMatchIndex() {
        return topMatchIndex;
    }

    public void setTopMatchIndex(int topMatchIndex) {
        this.topMatchIndex = topMatchIndex;
    }
}
