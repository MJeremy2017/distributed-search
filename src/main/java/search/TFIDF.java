package search;

import model.DocumentData;

import java.util.*;

public class TFIDF {
    public static DocumentData calculateTermFrequency(List<String> terms, List<String> words) {
        DocumentData termTFMap = new DocumentData();
        int totalCount = words.size();
        for (String term : terms) {
            int termCount = 0;
            for (String word : words) {
                if (term.equalsIgnoreCase(word)) {
                    termCount++;
                }
            }
            termTFMap.putTermFrequency(term, (double)termCount/totalCount);
        }
        return termTFMap;
    }

    public static Map<String, Double> calculateInverseDocumentFrequency(List<String> terms, Map<String, DocumentData> documents) {
        int numDocs = documents.size();
        HashMap<String, Double> termIDFMap = new HashMap<>();
        for (String document : documents.keySet()) {
            DocumentData documentData = documents.get(document);
            int termCount = 0;
            for (String term : terms) {
                Double termFrequency = documentData.getTermFrequency(term);
                if (termFrequency > 0) {
                    termCount++;
                }
                termIDFMap.put(term, termCount == 0 ? 0 : Math.log10((double) numDocs/termCount));

            }
        }
        return termIDFMap;
    }

    public static double calculateDocumentScore(List<String> terms,
                                                DocumentData documentData,
                                                Map<String, Double> termIDFMap) {
        double score = 0;
        for (String term : terms) {
            double tf = documentData.getTermFrequency(term);
            double idf = termIDFMap.get(term);
            score += tf * idf;
        }

        return score;
    }

    /**
     * Given search terms, calculate the scores for each document and stores them in a score to docs map
     * @param terms
     * @param allDocumentsData
     * @return
     */
    public static Map<Double, List<String>> calculateDocumentsScores(List<String> terms,
                                                                     Map<String, DocumentData> allDocumentsData) {
        // sorted map by key
        TreeMap<Double, List<String>> scoreToDoc = new TreeMap<>();
        Map<String, Double> termIDFMap = calculateInverseDocumentFrequency(terms, allDocumentsData);
        for (String docKey : allDocumentsData.keySet()) {
            DocumentData documentData = allDocumentsData.get(docKey);
            double score = calculateDocumentScore(terms, documentData, termIDFMap);

            List<String> docKeys = scoreToDoc.get(score);
            if (docKeys == null) {
                docKeys = new ArrayList<>();
            }
            docKeys.add(docKey);
            scoreToDoc.put(score, docKeys);
        }

        return scoreToDoc.descendingMap();
    }

    public static List<String> getWordsFromLines(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line : lines) {
            List<String> lineWords = getWordsFromLine(line);
            words.addAll(lineWords);
        }
        return words;
    }

    public static List<String> getWordsFromLine(String line) {
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }
}
