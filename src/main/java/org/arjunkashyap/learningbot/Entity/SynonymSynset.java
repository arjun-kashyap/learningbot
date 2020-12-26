package org.arjunkashyap.learningbot.Entity;

import edu.cmu.lti.jawjaw.pobj.Link;
import net.sf.extjwnl.data.Synset;

import java.util.Objects;

public class SynonymSynset extends Synonym {
    private Synset synset;
    private int useCount;

    public Synset getSynset() {
        return synset;
    }

    public void setSynset(Synset synset) {
        this.synset = synset;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynonymSynset that = (SynonymSynset) o;
        return synset.equals(that.synset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(synset);
    }

    @Override
    public String toString() {
        return "SynonymSynset{" +
                "synset=" + synset.getOffset() +
                ", useCount=" + useCount +
                '}';
    }
}