package search;

import model.DocumentData;
import model.Result;
import model.Task;
import networking.OnRequestCallback;
import serialization.SerializationUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SearchWorker implements OnRequestCallback {
    private List<String> searchTerms;
    private List<String> documents;
    private String TASK_ENDPOINT = "/task";

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        Task task = (Task) SerializationUtils.deserialize(requestPayload);
        if (task == null)
            return null;
        Result result = createResult(task);
        return SerializationUtils.serialize(result);
    }

    @Override
    public String getTaskEndpoint() {
        return TASK_ENDPOINT;
    }

    private Result createResult(Task task) {
        List<String> documents = task.getDocuments();
        System.out.println("Documents to process: " + documents.size());
        List<String> searchTerms = task.getSearchTerms();

        Result result = new Result();
        // get term frequency of each document
        for (String document : documents) {
            List<String> words = parseWordsFromDocument(document);
            DocumentData documentData = TFIDF.calculateTermFrequency(searchTerms, words);
            result.putDocData(document, documentData);
        }
        return result;
    }

    private List<String> parseWordsFromDocument(String document) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(document));
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            return TFIDF.getWordsFromLines(lines);
        } catch (FileNotFoundException e) {
            return Collections.emptyList();
        }
    }
}
