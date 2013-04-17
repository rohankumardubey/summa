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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubLeafImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

/**
 * Connects to a remote Solr installation using the REST API.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrLeaf extends HubLeafImpl {
    private static Log log = LogFactory.getLog(SolrLeaf.class);

    /**
     * The entry point for calls to Solr.
     * </p><p>
     * Optional. Default is localhost:8983 (Solr default).
     */
    public static final String CONF_URL = "solr.url";
    public static final String DEFAULT_URL = "http://localhost:8983";

    // TODO: Implement default qt
    // http://wiki.apache.org/solr/SolrRequestHandler#Handler_Resolution
    // public static final String CONF_QT = "solr.qt"

    // TODO: Timeouts

    private final String url;
    private final HttpSolrServer solrServer;

    public SolrLeaf(Configuration conf) {
        super(conf);
        url = conf.getString(CONF_URL, DEFAULT_URL);
        solrServer = new HttpSolrServer(url);
        log.info("Created " + this);
    }

    @Override
    public QueryResponse search(SolrParams params) throws SolrServerException {
        return solrServer.query(params);
    }

    public HttpSolrServer getSolrServer() {
        return solrServer;
    }

    public String toString() {
        return "SolrLeaf(" + super.toString() + ", url='" + url + "')";
    }
}
