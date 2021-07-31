package model;

import java.io.Serializable;
import java.util.Map;

public class Result implements Serializable {
    private Map<String, DocumentData> docToDocData;

    public void putDocData(String document, DocumentData documentData) {
        docToDocData.put(document, documentData);
    }

    public DocumentData getDocData(String document) {
        return docToDocData.get(document);
    }

    public Map<String, DocumentData> getDocToDocData() {
        return docToDocData;
    }
}
