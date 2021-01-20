package org.arjunkashyap.learningbot.Entity;

import edu.cmu.lti.jawjaw.pobj.Link;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class SynonymSynsetComparatorByType implements Comparator<SynonymSynset> {
    static Map<Link, Integer> mapper = new HashMap<>();
    static {
        mapper.put(Link.syns, 1);
        mapper.put(Link.sim, 2);
        mapper.put(Link.hype, 3);
        mapper.put(Link.hypo, 4);
    }
    @Override
    public int compare(SynonymSynset first, SynonymSynset second) {
        return mapper.get(first.getLinkType()).compareTo(mapper.get(second.getLinkType()));
    }
}