package com.example.demo.Scratch;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.OperatorSpec;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

public class ConstituentExample {

    public static void main(String[] args) {
        // set up pipeline properties

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,natlog, kbp");
        // use faster shift reduce parser
        props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        props.setProperty("parse.maxlen", "100");
        // set up Stanford CoreNLP pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        // build annotation for a review
        Scanner scanner = new Scanner(System.in);
        String myString = scanner.nextLine();
        Annotation annotation;
        while (!myString.equals("exit")) {
            annotation = new Annotation(myString);
            //(ROOT (SBARQ (WHNP (WP What)) (SQ (VBZ is) (RB not) (NP (DT an) (NN animal))) (. ?)))
            //(ROOT (SBARQ (WHNP (WP What)) (SQ (VBZ is) (NP (DT an) (NN animal))) (. ?)))
//        new Annotation("The small red car turned very quickly around the corner.");
            // annotate
            pipeline.annotate(annotation);
                // get tree
            Tree tree =
                    annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
            System.out.println(tree);
            Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
            for (Constituent constituent : treeConstituents) {
                if (constituent.label() != null &&
                        (constituent.label().toString().equals("VP") || constituent.label().toString().equals("NP"))) {
                    System.out.println("found constituent: " + constituent.toString());
                    System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end() + 1));
                }
            }

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap sentence : sentences) {
                // traversing the words in the current sentence
                // a CoreLabel is a CoreMap with additional token-specific methods
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    // this is the text of the token
                    String word = token.get(CoreAnnotations.TextAnnotation.class);
                    // this is the POS tag of the token
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                    // this is the NER label of the token
                    String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    // this is the polarity label of the token
                    String pol = token.get(NaturalLogicAnnotations.PolarityDirectionAnnotation.class);
                    OperatorSpec oper = token.get(NaturalLogicAnnotations.OperatorAnnotation.class);
                    System.out.print(word + " [" + pol + "] " + " [" + oper + "] ");
                }
                System.out.println("HERE");
                sentence.get(CoreAnnotations.KBPTriplesAnnotation.class).forEach(System.out::println);
                System.out.println("HERE2");
                System.out.println();
            }
            myString = scanner.nextLine();
        }
        scanner.close();
    }

}


//https://code.google.com/archive/p/ws4j/
//https://nlp.stanford.edu/courses/cs224n/2006/fp/henggong-telarson-joshd-1-cs224n_project_report.pdf
