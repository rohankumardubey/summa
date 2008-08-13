/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.control.service;

import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.LuceneSearcher;
import dk.statsbiblioteket.summa.search.document.DocumentResponse;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a {@link SummaSearcher}, which will normally translate to a
 * {@link LuceneSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchService extends ServiceBase {
    private Log log = LogFactory.getLog(SearchService.class);

    /**
     * The class to instantiate and use for searching.
     * </p><p>
     * This is optional. Default is the {@link LuceneSearcher} class.
     */
    public static final String CONF_SEARCHER_CLASS =
            "summa.search.searcher.class";

    public static final Class<? extends SummaSearcher> DEFAULT_SEARCHER_CLASS =
            LuceneSearcher.class;

    private Configuration conf;
    private SummaSearcher searcher;

    public SearchService(Configuration conf) throws IOException {
        super(conf);

        setStatus(Status.CODE.not_instantiated,
                  "Setting up",
                  Logging.LogLevel.DEBUG);

        this.conf = conf;
        exportRemoteInterfaces();

        setStatus(Status.CODE.constructed,
                  "Remote interfaces up",
                  Logging.LogLevel.DEBUG);
    }

    public synchronized void start() throws RemoteException {
        if (searcher != null) {
            log.debug("Start called on an already running searcher");
        }

        setStatusRunning("Creating Searcher");
        Class<? extends SummaSearcher> searcherClass;
        try {
            searcherClass = conf.getClass(CONF_SEARCHER_CLASS,
                                          SummaSearcher.class);
        } catch (NullPointerException e) {
            log.info(String.format(
                    "The property '%s' was not defined. Defaulting to '%s'",
                    CONF_SEARCHER_CLASS, DEFAULT_SEARCHER_CLASS));
            searcherClass = DEFAULT_SEARCHER_CLASS;
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The property '%s' with content '%s' could not be resolved "
                    + "to a proper class",
                    CONF_SEARCHER_CLASS, conf.getString(CONF_SEARCHER_CLASS));
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception constructing SummaSearcher class from the "
                    + "property '%s' with content '%s'",
                    CONF_SEARCHER_CLASS, conf.getString(CONF_SEARCHER_CLASS));
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }

        log.debug(String.format(
                "Got SummaSearcher class '%s'. Commencing creation",
                searcherClass));

        try {
            searcher = Configuration.create(searcherClass, conf);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "The SummaSearcher-class '%s' was not a Configurable",
                    searcherClass);
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        } catch (Exception e) {
            String message = String.format(
                    "Exception creating instance of SummaSearcher class '%s'",
                    searcherClass);
            setStatus(Status.CODE.crashed, message, Logging.LogLevel.ERROR, e);
            throw new RemoteException(message, e);
        }

        setStatusIdle();
    }

    public synchronized void stop() throws RemoteException {
        if (searcher == null) {
            log.debug("stop called, but status is already stopped");
            return;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            searcher.close();
            setStatus(Status.CODE.stopped, "Searcher closed successfully",
                      Logging.LogLevel.DEBUG);
        } catch (Exception e) {
            setStatus(Status.CODE.crashed, "Searcher closed with error",
                      Logging.LogLevel.WARN, e);
            throw new RemoteException(String.format(
                    "Unable to close searcher '%s'", searcher), e);
        } finally {
            //noinspection AssignmentToNull
            searcher = null;
        }
    }

}
