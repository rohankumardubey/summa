package dk.statsbiblioteket.summa.support.harmonise;

import de.schlichtherle.truezip.entry.Entry;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.ExtraAsserts;
import dk.statsbiblioteket.summa.search.SummaSearcherAggregator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.tools.ant.types.resources.selectors.InstanceOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResponseMergerTest extends TestCase {
    public ResponseMergerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ResponseMergerTest.class);
    }

    public void testInterleave() {
        assertEquals(Configuration.newMemoryBased(
            ResponseMerger.CONF_MODE, ResponseMerger.MERGE_MODE.interleave,
            ResponseMerger.CONF_ORDER, "searcherB, searcherC"),
            new Request(), generateResponses(), Arrays.asList(
            "B1", "C1", "A1", "B2", "C2", "A2", "B3", "C3", "A3"
        ));
    }

    public void testConcatenate() {
        assertEquals(Configuration.newMemoryBased(
            ResponseMerger.CONF_MODE, ResponseMerger.MERGE_MODE.concatenate,
            ResponseMerger.CONF_ORDER, "searcherC, searcherA"),
            new Request(), generateResponses(), Arrays.asList(
            "C1", "C2", "C3", "A1", "A2", "A3", "B1", "B2", "B3"
        ));
    }

    // "A1", 1.0f, "A2", 1.0f, "A3", 0.1f
    // "B1", 0.7f, "B2", 0.5f, "B3", 0.1f
    // "C1", 0.2f, "C2", 0.1f, "C3", 0.01f
    public void testScore() {
        assertEquals(Configuration.newMemoryBased(
            ResponseMerger.CONF_MODE, ResponseMerger.MERGE_MODE.score),
            new Request(), generateResponses(), Arrays.asList(
            "A1", "A2", "B1", "B2", "C1", "A3", "B3", "C2", "C3"
        ));
    }


    private void assertEquals(
        Configuration conf, Request request,
        List<SummaSearcherAggregator.ResponseHolder> responses,
        List<String> expected) {
        ResponseMerger merger = new ResponseMerger(conf);
        ResponseCollection mergedResponses = merger.merge(request, responses);
        for (Response merged: mergedResponses) {
            if (merged instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)merged;
                List<String> actual =
                    new ArrayList<String>(docs.getRecords().size());
                for (DocumentResponse.Record record: docs.getRecords()) {
                    actual.add(record.getId());
                }
                ExtraAsserts.assertEquals(
                    "The returned IDs should be as expected", expected, actual);
                return;
            }
        }
        fail("No DocumentResponse found in responses");
    }

    // "A1", 1.0f, "A2", 1.0f, "A3", 0.1f
    // "B1", 0.7f, "B2", 0.5f, "B3", 0.1f
    // "C1", 0.2f, "C2", 0.1f, "C3", 0.01f
    /**
     * @return sample data from three simulated searchers.
     */
    @SuppressWarnings({"RedundantArrayCreation"})
    private List<SummaSearcherAggregator.ResponseHolder> generateResponses() {
        List<SummaSearcherAggregator.ResponseHolder> holders =
            new ArrayList<SummaSearcherAggregator.ResponseHolder>(3);
        Request request = new Request(
            DocumentKeys.SEARCH_FILTER, null,
            DocumentKeys.SEARCH_QUERY, "foo",
            DocumentKeys.SEARCH_MAX_RECORDS, 20);
        List<Object> hitsA = Arrays.asList(new Object[]{
            "A1", 1.0f,
            "A2", 1.0f,
            "A3", 0.1f
        });
        holders.add(generateResponse("searcherA", request, 87, 123, hitsA));
        List<Object> hitsB = Arrays.asList(new Object[]{
            "B1", 0.7f,
            "B2", 0.5f,
            "B3", 0.1f
        });
        holders.add(generateResponse("searcherB", request, 12, 321, hitsB));
        List<Object> hitsC = Arrays.asList(new Object[]{
            "C1", 0.2f,
            "C2", 0.1f,
            "C3", 0.01f
        });
        holders.add(generateResponse("searcherC", request, 3, 43, hitsC));
        return holders;
    }

    /**
     * Generate a fake response meant for further processing by the
     * ResponseMerger.
     * @param searcherID the ID for the searcher. This should correspond to the
     *        setup for the ResponseMerger.
     * @param request the simulated request producing the records.
     * @param hitCount the simulated total number of hits. This is equal to or
     *        more than the number of records.
     * @param searchTime simultated search time in ms.
     * @param records alternations of RecordID (String) and score (float).
     * @return a thin response, with a fake DocumentResponse inside.
     */
    @SuppressWarnings({"NullableProblems"})
    private SummaSearcherAggregator.ResponseHolder generateResponse(
        String searcherID, Request request, int hitCount, long searchTime,
        List<Object> records) {
        ResponseCollection collection = new ResponseCollection();
        DocumentResponse docs = new DocumentResponse(
            request.getString(DocumentKeys.SEARCH_FILTER, null),
            request.getString(DocumentKeys.SEARCH_QUERY, null),
            0, request.getInt(DocumentKeys.SEARCH_MAX_RECORDS, 20),
            DocumentKeys.SORT_ON_SCORE, false,
            new String[]{DocumentKeys.RECORD_ID}, searchTime, hitCount);
        collection.add(docs);
        for (int i = 0; i < records.size(); i += 2) {
            String id = (String)records.get(i);
            float score = (Float)records.get(i+1);
            DocumentResponse.Record record = new DocumentResponse.Record(
                id, searcherID, score, DocumentKeys.SORT_ON_SCORE);
            record.addField(new DocumentResponse.Field(
                DocumentKeys.RECORD_ID, id, true));
            docs.addRecord(record);
        }
        return new SummaSearcherAggregator.ResponseHolder(
            searcherID, request, collection);
    }
}
