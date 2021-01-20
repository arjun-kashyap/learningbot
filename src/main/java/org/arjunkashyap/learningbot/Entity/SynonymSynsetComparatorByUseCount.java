package org.arjunkashyap.learningbot.Entity;

import edu.cmu.lti.jawjaw.pobj.Link;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SynonymSynsetComparatorByUseCount implements Comparator<SynonymSynset> {
    @Override
    public int compare(SynonymSynset first, SynonymSynset second) {
        return second.getUseCount()-first.getUseCount();
    }
}