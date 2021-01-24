package org.arjunkashyap.learningbot.Entity;

import net.sf.extjwnl.data.Synset;

import java.util.Comparator;

public class SynsetComparatorByOffset implements Comparator<Synset> {
    @Override
    public int compare(Synset first, Synset second) {
        return Long.compare(second.getOffset(), first.getOffset());
    }
}
