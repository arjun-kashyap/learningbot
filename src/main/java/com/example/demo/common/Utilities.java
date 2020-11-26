package com.example.demo.common;

import com.example.demo.Entity.Synonym;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Utilities {
    public static List<List<Synonym>> getAllCombinations(List<Set<Synonym>> listOfSetOfSynonyms) {
        //System.out.println("HERE: " + listOfSetOfSynonyms);
        List<List<Synonym>> allCombinations = new ArrayList<>();
        if (listOfSetOfSynonyms.size() == 1) {
            for (Synonym synonym : listOfSetOfSynonyms.get(0)) {
                List<Synonym> y = new ArrayList<Synonym>();
                y.add(synonym);
                allCombinations.add(y);
            }
        } else {
            Set<Synonym> synonymsOfFirstWord = listOfSetOfSynonyms.get(0);
            List<List<Synonym>> listOfSynOfRestOfWords = getAllCombinations(listOfSetOfSynonyms.subList(1, listOfSetOfSynonyms.size()));
            for (List<Synonym> previousCombinations : listOfSynOfRestOfWords) {
                for (Synonym synonym : synonymsOfFirstWord) {
                    List<Synonym> combination = new ArrayList<>();
                    combination.add(synonym);
                    combination.addAll(previousCombinations);
                    allCombinations.add(combination);
                }
            }
        }
        return allCombinations;
    }

}
