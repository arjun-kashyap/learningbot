package com.example.demo.Entity;

import java.util.List;

public class SynsetsWords {
    private List<String> synsets;
    private List<Synonym> synonyms;

    public List<String> getSynsets() {
        return synsets;
    }

    public void setSynsets(List<String> synsets) {
        this.synsets = synsets;
    }

    public List<Synonym> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<Synonym> synonyms) {
        this.synonyms = synonyms;
    }
}
