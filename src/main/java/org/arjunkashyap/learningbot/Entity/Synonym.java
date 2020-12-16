package org.arjunkashyap.learningbot.Entity;

import edu.cmu.lti.jawjaw.pobj.Link;

import java.util.Objects;

public class Synonym extends Word implements Comparable<Synonym> {
    private int score;
    private Link linkType;

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Link getLinkType() {
        return linkType;
    }

    public void setLinkType(Link linkType) {
        this.linkType = linkType;
    }

    @Override
    public String toString() {
        return "Synonym{" +
                "word='" + word + '\'' +
                ", score=" + score +
                ", linkType=" + linkType +
                ", pos=" + pos +
                '}';
    }

    @Override
    public boolean equals(Object o) { //TODO: equals and compareto do not use the same variables. Check if this will cause issue
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Synonym synonym = (Synonym) o;
        return word.equals(synonym.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word);
    }

    @Override
    public int compareTo(Synonym o) {
        return (score-o.score);
    }
}