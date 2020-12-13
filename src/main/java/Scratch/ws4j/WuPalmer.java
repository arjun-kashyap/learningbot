package Scratch.ws4j;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.util.DepthFinder.Depth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WuPalmer extends RelatednessCalculator {
    private static List<POS[]> posPairs = new ArrayList<POS[]>() {
        {
            this.add(new POS[]{POS.n, POS.n});
            this.add(new POS[]{POS.v, POS.v});
        }
    };

    public WuPalmer(ILexicalDatabase db) {
        super(db);
    }

    protected Relatedness calcRelatedness(Concept synset1, Concept synset2) {
        StringBuilder tracer = new StringBuilder();
        if (synset1 != null && synset2 != null) {
            if (synset1.getSynset().equals(synset2.getSynset())) {
                return new Relatedness(1, "Synsets are identical.", (String)null);
            } else {
                StringBuilder subTracer = enableTrace ? new StringBuilder() : null;
                List<Depth> lcsList = this.depthFinder.getRelatedness(synset1, synset2, subTracer);
                if (lcsList.size() == 0) {
                    return new Relatedness(0);
                } else {
                    int depth = ((Depth)lcsList.get(0)).depth;
                    int depth1 = this.depthFinder.getShortestDepth(synset1);
                    int depth2 = this.depthFinder.getShortestDepth(synset2);
                    double score = 0.0D;
                    if (depth1 > 0 && depth2 > 0) {
                        score = (double)(2 * depth) / (double)(depth1 + depth2);
                        System.out.println(score + " " + depth + " " + depth1 + " " + depth2 + " "+lcsList);
                    }

                    if (enableTrace) {
                        tracer.append(subTracer.toString());
                        Iterator i$ = lcsList.iterator();

                        while(i$.hasNext()) {
                            Depth lcs = (Depth)i$.next();
                            tracer.append("Lowest Common Subsumer(s): ");
                            tracer.append(this.db.conceptToString(lcs.leaf) + " (Depth=" + lcs.depth + ")\n");
                        }

                        tracer.append("Depth1( " + this.db.conceptToString(synset1.getSynset()) + " ) = " + depth1 + "\n");
                        tracer.append("Depth2( " + this.db.conceptToString(synset2.getSynset()) + " ) = " + depth2 + "\n");
                    }

                    return new Relatedness(score, tracer.toString(), (String)null);
                }
            }
        } else {
            return new Relatedness(0, (String)null, "Synset is null.");
        }
    }

    public List<POS[]> getPOSPairs() {
        return posPairs;
    }
}
