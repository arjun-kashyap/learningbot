package org.arjunkashyap.learningbot.Entity;

import java.util.Objects;

public class WordClassification {
    private String word;
    private String lemma;
    private String pos;

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

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WordClassification that = (WordClassification) o;
        return Objects.equals(word, that.word) &&
                Objects.equals(lemma, that.lemma) &&
                pos == that.pos;
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
