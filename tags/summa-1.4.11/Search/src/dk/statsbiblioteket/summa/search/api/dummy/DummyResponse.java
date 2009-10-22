package dk.statsbiblioteket.summa.search.api.dummy;

import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.util.Strings;

import java.util.ArrayList;

/**
 * {@link Response} object generated by {@link dk.statsbiblioteket.summa.search.dummy.SummaSearcherDummy} and
 * {@link dk.statsbiblioteket.summa.search.dummy.SearchNodeDummy}.
 */
public class DummyResponse implements Response {

    protected String warmUps;
    protected String opens;
    protected String closes;
    protected String searches;
    protected ArrayList<String> ids;

    public DummyResponse (String id, int warmUps, int opens, int closes,
                          int searches) {
        this.warmUps = "" + warmUps;
        this.opens = "" + opens;
        this.closes = "" + closes;
        this.searches = "" + searches;
        ids = new ArrayList<String>(10);
        ids.add(id);
    }

    public String getName () {
        return "DummyResponse";
    }

    public void merge (Response other) throws ClassCastException {
        DummyResponse resp = (DummyResponse)other;
        warmUps += ", " + resp.warmUps;
        opens += ", " + resp.opens;
        closes += ", " + resp.closes;
        searches += ", " + resp.searches;
        ids.addAll(resp.ids);
    }

    public String toXML () {
        return String.format ("<DummyResponse>\n" +
                              "  <warmUps>%s</warmUps>\n"+
                              "  <ids>%s</ids>\n"+
                              "  <opens>%s</opens>\n"+
                              "  <closes>%s</closes>\n"+
                              "  <searches>%s</searches>\n"+
                              "</DummyResponse>",
                              warmUps, Strings.join(ids, ", "), opens, closes,
                              searches);
    }

    public ArrayList<String> getIds() {
        return ids;
    }
}



