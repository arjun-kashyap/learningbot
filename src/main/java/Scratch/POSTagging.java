package Scratch;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;

import java.util.*;

public class POSTagging {

    public static String text = "Abraham Lincoln.";

    public static void main(String[] args) {
        // set up pipeline properties
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos");
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // create a document object
        CoreDocument document = pipeline.processToCoreDocument(text);
        // display tokens
        for (CoreLabel tok : document.tokens()) {
            System.out.println(String.format("%s\t%s", tok.word(), tok.tag()));
        }
    }
}


//https://code.google.com/archive/p/ws4j/
//https://nlp.stanford.edu/courses/cs224n/2006/fp/henggong-telarson-joshd-1-cs224n_project_report.pdf
//Core NLP Tags - https://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html