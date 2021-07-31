package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentData implements Serializable {
    private Map<String, Double> DocumentData = new HashMap<>();

    public void putTermFrequency(String term, Double frequency) {
        DocumentData.put(term, frequency);
    }

    public Double getTermFrequency(String term) {
        return DocumentData.get(term);
    }
 }
