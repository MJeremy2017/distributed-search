import model.DocumentData;
import search.TFIDF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

public class SequentialSearch {
    private static final String SEARCH_QUERY = "The best detective";
    private static final String DIR_PATH = "./resources/books";

    public static void main(String[] args) throws FileNotFoundException {
        Map<String, List<String>> documents = getDocuments(DIR_PATH);
        List<String> terms = TFIDF.getWordsFromLine(SEARCH_QUERY);
        Map<String, DocumentData> allDocumentsData = new HashMap<>();
        for (String document : documents.keySet()) {
            List<String> words = documents.get(document);
            DocumentData documentData = TFIDF.calculateTermFrequency(terms, words);
            allDocumentsData.put(document, documentData);
        }

        Map<Double, List<String>> scoreToDocs = TFIDF.calculateDocumentsScores(terms, allDocumentsData);
        printScores(scoreToDocs);
    }

    private static void printScores(Map<Double, List<String>> scoreToDocs) {
        for (Map.Entry<Double, List<String>> entry : scoreToDocs.entrySet()) {
            Double score = entry.getKey();
            for (String doc : entry.getValue()) {
                System.out.printf("document: %s - score %f\n", doc, score);
            }
        }
    }

    private static Map<String, List<String>> getDocuments(String dirPath) throws FileNotFoundException {
        File rootDir = new File(dirPath);
        HashMap<String, List<String>> documents = new HashMap<>();
        List<String> files = Arrays.stream(rootDir.list())
                .map(x -> dirPath + "/" + x)
                .collect(Collectors.toList());

        for (String fileName : files) {
            File file = new File(fileName);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            List<String> words = TFIDF.getWordsFromLines(lines);
            documents.put(fileName.split("/")[3], words);
        }

        return documents;
    }


}
