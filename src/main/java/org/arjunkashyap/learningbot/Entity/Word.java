package org.arjunkashyap.learningbot.Entity;

import java.util.Objects;

public class Word {
    protected String word;
    protected String lemma;
    protected BotPOS pos;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word that = (Word) o;
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
