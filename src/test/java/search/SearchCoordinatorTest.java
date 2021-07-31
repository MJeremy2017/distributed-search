package search;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchCoordinatorTest {
    private static SearchCoordinator searchCoordinator;

    @BeforeAll
    public static void  Setup() {
        searchCoordinator = new SearchCoordinator(null, null);
    }

    @Test
    void TestSplitDocuments() {
        List<String> documents = new ArrayList<>(Arrays.asList("doc1", "doc2", "doc3", "doc4", "doc5"));
        List<List<String>> expected = Arrays.asList(
                Arrays.asList("doc1", "doc2"),
                Arrays.asList("doc3", "doc4"),
                Collections.singletonList("doc5"));
        int numWorkers = 3;
        List<List<String>> results = SearchCoordinator.splitDocuments(documents, numWorkers);
        assertEquals(expected, results);
    }


}