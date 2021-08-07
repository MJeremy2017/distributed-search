package search;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.source.util.TaskEvent;
import management.ServiceRegistry;
import model.DocumentData;
import model.Result;
import model.Task;
import model.proto.SearchModel;
import networking.OnRequestCallback;
import networking.WebClient;
import serialization.SerializationUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Receive search request from frontend server and distribute tasks to java workers,
task distribution using java serializer while frontend server connection using protobuf to communicate.
 */
public class SearchCoordinator implements OnRequestCallback {
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String BOOKS_DIR = "./resources/books";
    private final ServiceRegistry workerServiceRegistry;
    private final WebClient webClient;

    public SearchCoordinator(ServiceRegistry workerServiceRegistry, WebClient webClient) {
        this.workerServiceRegistry = workerServiceRegistry;
        this.webClient = webClient;
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestPayload);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return SearchModel.Response.getDefaultInstance().toByteArray();
        }
    }

    private SearchModel.Response createResponse(SearchModel.Request request) {
        String searchQuery = request.getSearchQuery();
        System.out.println("Received search query: " + searchQuery);
        List<String> searchTerms = TFIDF.getWordsFromLine(searchQuery);

        // split docs into several tasks
        List<String> documents = getDocumentList();
        List<String> workerAddresses = workerServiceRegistry.getWorkerAddresses();
        int numWorkers = workerAddresses.size();

        List<List<String>> eachWorkerDocuments = splitDocuments(documents, numWorkers);
        List<Task> tasks = createTasks(searchTerms, eachWorkerDocuments);
        List<Result> resultList = sendTasksToWorkers(workerAddresses, tasks);

        List<SearchModel.Response.DocumentStats> documentStats = aggregateDocumentsScore(searchTerms, resultList);
        // build Response

        return SearchModel.Response.newBuilder()
                .addAllRelevantDocuments(documentStats)
                .build();
    }

    private List<SearchModel.Response.DocumentStats> aggregateDocumentsScore(List<String> searchTerms, List<Result> resultList) {
        // aggregate results into document data
        Map<String, DocumentData> allDocumentResults = new HashMap<>();
        for (Result result : resultList) {
            allDocumentResults.putAll(result.getDocToDocData());
        }
        Map<Double, List<String>> scoreToDocs = TFIDF.calculateDocumentsScores(searchTerms, allDocumentResults);

        List<SearchModel.Response.DocumentStats> documentStatsList = new ArrayList<>();
        for (Map.Entry<Double, List<String>> entry : scoreToDocs.entrySet()) {
            double score = entry.getKey();
            List<String> docs = entry.getValue();
            for (String doc : docs) {
                SearchModel.Response.DocumentStats stats = SearchModel.Response.DocumentStats.newBuilder()
                        .setDocumentName(doc)
                        .setScore(score)
                        .build();
                documentStatsList.add(stats);
            }
        }

        return documentStatsList;
    }

    private List<Result> sendTasksToWorkers(List<String> workerAddresses, List<Task> tasks) {
        List<CompletableFuture<Result>> futureList = new ArrayList<>();
        for (int i=0; i<tasks.size(); ++i) {
            String url = workerAddresses.get(i);
            Task task = tasks.get(i);
            byte[] taskBytes = SerializationUtils.serialize(task);
            CompletableFuture<Result> resultFuture = webClient.sendTask(url, taskBytes);
            futureList.add(resultFuture);
        }

        List<Result> resultList = new ArrayList<>();
        for (CompletableFuture<Result> future : futureList) {
            try {
                Result result = future.get();
                resultList.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("Received %d/%d results", resultList.size(), tasks.size());
        return resultList;
    }

    private List<Task> createTasks(List<String> searchTerms, List<List<String>> eachWorkerDocuments) {
        List<Task> tasks = new ArrayList<>();
        for (List<String> docs : eachWorkerDocuments) {
            Task task = new Task(searchTerms, docs);
            tasks.add(task);
        }
        return tasks;
    }

    public static List<List<String>> splitDocuments(List<String> documents, int size) {
        List<Integer> numDocumentsPerWorker = new ArrayList<>();
        List<List<String>> eachWorkerDocuments = new ArrayList<>();
        int numTasksEachSplit = documents.size() / size;
        int extraDocs = documents.size() % size;
        for (int i=0; i<size; ++i) {
            if (extraDocs > 0) {
                numDocumentsPerWorker.add(numTasksEachSplit + 1);
                extraDocs--;
            } else {
                numDocumentsPerWorker.add(numTasksEachSplit);
            }
        }

        int startIndex = 0;
        int endIndex = 0;
        for (int i=0; i<size; ++i) {
            endIndex += numDocumentsPerWorker.get(i);
            eachWorkerDocuments.add(documents.subList(startIndex, endIndex));
            startIndex = endIndex;
        }
        return eachWorkerDocuments;
    }

    private List<String> getDocumentList() {
        File file = new File(BOOKS_DIR);
        List<String> documentList = Stream.of(file.list())
                .map(x -> BOOKS_DIR + "/" + x)
                .collect(Collectors.toList());
        return documentList;
    }

    @Override
    public String getTaskEndpoint() {
        return SEARCH_ENDPOINT;
    }
}
