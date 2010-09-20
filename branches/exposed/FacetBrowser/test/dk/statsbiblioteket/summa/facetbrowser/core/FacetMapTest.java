/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.facetbrowser.core;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.facetbrowser.FacetStructure;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMapBitStuffed;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.StringWriter;
import java.util.*;

/**
 * FacetMap Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FacetMapTest extends TestCase {
    public FacetMapTest(String name) {
        super(name);
    }

    private FacetMap map;
    private CoreMap core;
    private TagHandler handler;

    public void setUp() throws Exception {
        super.setUp();

        MemoryStorage memStore = new MemoryStorage();
        List<ConfigurationStorage> subs =
                memStore.createSubStorages(Structure.CONF_FACETS, 3);
        int counter = 0;
        for (String facetName: new String[]{"A", "B", "C"}) {
            subs.get(counter++).put(FacetStructure.CONF_FACET_NAME, facetName);
        }
        Configuration config = new Configuration(memStore);
        Structure structure = new Structure(config);
        assertEquals("The Structure should contain 3 Facets",
                     3, structure.getFacets().size());
        core = new CoreMapBitStuffed(config, structure);
        handler = new TagHandlerImpl(config, structure, false);
        map = new FacetMap(structure, core, handler, false);
        map.open(TMP_DIR);
    }
    private File TMP_DIR = new File(System.getProperty("java.io.tmpdir"),
                                    "testTagHandler");
    public void tearDown() throws Exception {
        super.tearDown();
        //noinspection AssignmentToNull
        map.close();
        handler.close();
        if (TMP_DIR.exists()) {
            Files.delete(TMP_DIR);
        }
    }

    public Map<String, List<String>> arraysToMap(String[] facets,
                                                 String[][] tags) {
        Map<String, List<String>> result =
                new HashMap<String, List<String>>(facets.length);
        int facetID = 0;
        for (String facet: facets) {
            result.put(facet, Arrays.asList(tags[facetID++]));
        }
        return result;
    }

    public void testGetDocCount() throws Exception {
        assertEquals("Initial count should be 0", 0, map.getDocCount());
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo", "ABar"}}));
        assertEquals("Adding one should give 1", 1, map.getDocCount());
        map.addToDocument(0, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BFoo", "BBar"}}));
        assertEquals("Updating should still give 1", 1, map.getDocCount());
        map.addToDocument(1, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BZoo", "BBar"}}));
        assertEquals("Adding another should give 2", 2, map.getDocCount());
        map.removeDocument(0);
        assertEquals("Removing a document should not decrease the count with no"
                     + " shift (default)",
                     2, map.getDocCount());
    }

    protected String dump(String[] values) {
        StringWriter sw = new StringWriter(values.length * 4);
        sw.append("[");
        for (int i = 0 ; i < values.length ; i++) {
            sw.append(values[i]);
            if (i < values.length - 1) {
                sw.append(", ");
            }
        }
        sw.append("]");
        return sw.toString();
    }

    public void assertHasTags(String message, int docID, String facet,
                              String[] expectedTags) throws Exception {
        Arrays.sort(expectedTags);
        int facetID = handler.getFacetID(facet);
        if (facetID == -1) {
            fail(message + ". Facet \"" + facet + "\" was not present");
        }
        int[] tagIDs = core.get(docID, facetID);
        String[] tagNames = new String[tagIDs.length];
        for (int i = 0 ; i < tagIDs.length ; i++) {
            tagNames[i] = handler.getTagName(facetID, tagIDs[i]);
        }
        if (Arrays.equals(expectedTags, tagNames)) {
            return;
        }
        fail(message + ". Expected: " + dump(expectedTags)
             + " got " + dump(tagNames));
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testCorrectness() throws Exception {
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo",
                                                         "ABar",
                                                         "AZoo",
                                                         "ABoo"}}));
        assertHasTags("Facet A should be added", 0, "A", 
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo"});
        map.addToDocument(0, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AKazam"}}));
        assertHasTags("Facet A should have AKazam added", 0, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo", "AKazam"});
        map.addToDocument(0, arraysToMap(new String[]{"B"},
                                         new String[][]{{"BKazam"}}));
        assertHasTags("Facet B should have BKazam only", 0, "B",
                      new String[]{"BKazam"});
        map.addToDocument(1, arraysToMap(new String[]{"A"},
                                         new String[][]{{"AFoo",
                                                         "ABar",
                                                         "AZoo",
                                                         "ABii"}}));
        assertHasTags("Facet 1/A should be added", 1, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABii"});
        assertHasTags("Facet 0/A should be unchanged", 0, "A", 
                      new String[]{"AFoo", "ABar", "AZoo", "ABoo", "AKazam"});
/*        map.removeDocument(0);
        assertHasTags("Facet 1/A should now be 0/A", 0, "A",
                      new String[]{"AFoo", "ABar", "AZoo", "ABii"});
        try {
            core.get(1, 0);
            fail("Document 1 should no longer exist");
        } catch(Exception e) {
            // Expected
        }*/
    }

    public void testInsert() throws Exception {
        map.addToDocument(0, arraysToMap(
                new String[]{"A"},
                new String[][]{{"AFoo", "ABar"}}));
        assertHasTags("Doc 0 Facet A should be added", 0, "A",
                      new String[]{"AFoo", "ABar"});
        map.addToDocument(1, arraysToMap(
                new String[]{"A"},
                new String[][]{{"AFoo", "AAbe"}}));
        assertHasTags("Doc 1 Facet A should be added", 1, "A",
                      new String[]{"AFoo", "AAbe"});
        assertHasTags("Doc 0 Facet A should be unchanged", 0, "A",
                      new String[]{"AFoo", "ABar"});
    }

    public void testInsertOrder() throws Exception {
        int RUNS = 100;
        String[][][] INSERTS = new String[][][] {
                {{"1A"}, {"1"}},
                {{"1B"}, {"3"}},
                {{"3B"}, {"5"}},
                {{"0A"}, {"2"}},
                {{"0A"}, {"3"}},
                {{"0A"}, {"2"}},
                {{"0B"}, {"4"}}
        };
        for (int i = 0 ; i < RUNS ; i++) {
            handler.clearTags();
            core.clear();
            permute(INSERTS);
            insertTags(INSERTS);
            assertHasTags("Doc 0 Facet A should be right", 0, "A",
                          new String[]{"2", "3"});
            assertHasTags("Doc 0 Facet B should be right", 0, "B",
                          new String[]{"4"});
            assertHasTags("Doc 1 Facet A should be right", 1, "A",
                          new String[]{"1"});
            assertHasTags("Doc 1 Facet b should be right", 1, "B",
                          new String[]{"3"});
            assertHasTags("Doc 3 Facet A should be right", 3, "B",
                          new String[]{"5"});
        }
    }

    private void permute(String[][][] inserts) {
        Random random = new Random();
        for (int i = 0 ; i < inserts.length ; i++) {
            String[][] t = inserts[i];
            int pos = random.nextInt(inserts.length);
            inserts[i] = inserts[pos];
            inserts[pos] = t;
        }
    }

    public void testInsertMultiple() throws Exception {
        String[][][] INSERTS = new String[][][] {
                {{"0A"}, {"2"}},
                {{"0A"}, {"3"}},
                {{"0A"}, {"2"}},
                {{"0B"}, {"4"}},
                {{"1A"}, {"1"}},
                {{"1B"}, {"3"}},
                {{"3B"}, {"5"}}
        };
        insertTags(INSERTS);
        assertHasTags("Doc 0 Facet A should be right", 0, "A",
                      new String[]{"2", "3"});
        assertHasTags("Doc 0 Facet B should be right", 0, "B",
                      new String[]{"4"});
        assertHasTags("Doc 1 Facet A should be right", 1, "A",
                      new String[]{"1"});
        assertHasTags("Doc 1 Facet b should be right", 1, "B",
                      new String[]{"3"});
        assertHasTags("Doc 3 Facet A should be right", 3, "B",
                      new String[]{"5"});
    }

    private void insertTags(String[][][] INSERTS) {
        for (String[][] insert: INSERTS) {
            String pos = insert[0][0];
            int docID = Integer.parseInt(pos.substring(0, 1));
            String facetName = pos.substring(1, 2);
            String[] tags = insert[1];
            map.addToDocument(docID, arraysToMap(
                    new String[]{facetName},
                    new String[][]{tags}));
        }
    }

    public void testPersistence() throws Exception {
        testInsert();
        map.store();
        map.addToDocument(0, arraysToMap(
                new String[]{"B"},
                new String[][]{{"AFoo"}}));
        assertHasTags("Doc 0 Facet B should created", 0, "B",
                      new String[]{"AFoo"});
        map.open(TMP_DIR);
        assertHasTags("Doc 0 Facet A should be fine after persistence", 0, "A",
                      new String[]{"AFoo", "ABar"});
        assertHasTags("Doc 1 Facet A should be fine after persistence", 1, "A",
                      new String[]{"AFoo", "AAbe"});
        int facetID = handler.getFacetID("B");
        int[] tagIDs = core.get(0, facetID);
        assertEquals("Doc 0 Facet B should have no tags after open", 
                     0, tagIDs.length);
    }

    public static Test suite() {
        return new TestSuite(FacetMapTest.class);
    }
}




