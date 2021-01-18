package org.arjunkashyap.learningbot.Entity;

import edu.cmu.lti.jawjaw.pobj.Link;

import java.util.Objects;

public class BotWord {
    protected String word;
    protected String lemma;
    protected BotPOS pos;
    protected Link linkType;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public BotPOS getPos() {
        return pos;
    }

    public void setPos(BotPOS pos) {
        this.pos = pos;
    }

    public Link getLinkType() {
        return linkType;
    }

    public void setLinkType(Link linkType) {
        this.linkType = linkType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotWord that = (BotWord) o;
        return word.equals(that.word) &&
                lemma.equals(that.lemma) &&
                pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word, lemma, pos);
    }

    @Override
    public String toString() {
        return "WordClassification{" +
                "word='" + word + '\'' +
                ", lemma='" + lemma + '\'' +
                ", pos=" + pos +
                '}';
    }
}
