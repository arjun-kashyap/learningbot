package org.arjunkashyap.learningbot.common;

import org.arjunkashyap.learningbot.Entity.Synonym;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Component
public class Utilities<T> {
    public static int[][] getAllCombinations(int[] input, int product) {
        System.out.println("HEAP: "+product+" "+input.length);
        int[][] combinations = new int[input.length][product];
        int repeat = product;
        for (int i = 0; i<input.length;i++) {
            repeat = repeat/input[i];
            for (int j = 0; j<product;j++) {
                combinations[i][j] = (j / repeat) % input[i];
            }
        }
        return combinations;
    }

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


    public String serializeToString(T myObject) {
        String serializedObject = "";
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(myObject);
            so.flush();
            //serializedObject = bo.toString();
            serializedObject = Base64.getEncoder().encodeToString(bo.toByteArray());
        } catch (Exception e) {
            System.out.println(e);
        }
        return serializedObject;
    }

    public T deserializeFromString(String serializedObject) {
        T obj = null;
        try {
            //byte b[] = serializedObject.getBytes();
            byte b[] = Base64.getDecoder().decode(serializedObject);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = (T) si.readObject();
        } catch (Exception e) {
            System.out.println(e);
        }
        return obj;
    }
}
