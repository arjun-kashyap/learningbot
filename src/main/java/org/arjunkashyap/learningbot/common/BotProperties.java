package org.arjunkashyap.learningbot.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Component
public class BotProperties extends Properties {
    @Autowired
    private Environment env;
    public String INDEX_LOCATION;
    public int HITS_THRESHOLD;
    public int COMBINATIONS_THRESHOLD;

    public BotProperties() {
        super();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        setProperty("annotators", env.getProperty("corenlp.annotators"));
        setProperty("parse.model", env.getProperty("corenlp.parse.model"));
        setProperty("parse.maxlen", env.getProperty("corenlp.parse.maxlen"));
        setProperty("ner.applyFineGrained", "true");
        setProperty("mention.type", "dep");
        INDEX_LOCATION = env.getProperty("lucene.index.path");
        HITS_THRESHOLD = Integer.parseInt(env.getProperty("search.totalHitsThreshold"));
        COMBINATIONS_THRESHOLD = Integer.parseInt(env.getProperty("index.maxCombinations"));
    }
}