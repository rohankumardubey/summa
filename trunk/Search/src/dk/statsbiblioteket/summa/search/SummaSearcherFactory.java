package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class used to instantiate the correct {@link SummaSearcher}
 * given a {@link Configuration}
 */
public class SummaSearcherFactory {

    private static final Log log = LogFactory.getLog (SummaSearcherFactory.class);

    /**
     * Create a {@link SummaSearcher} instance of the class specified by the
     * {@link SummaSearcher#PROP_CLASS} in {@code conf}. If this property
     * is not defined the method will default to a {@link SummaSearcherImpl}
     *
     * @param conf the configuration used to look up
     *             {@link SummaSearcher#PROP_CLASS}
     * @param defaultClass the class to instantiate if
     *                     {@link SummaSearcher#PROP_CLASS} is not set in
     *                     {@code conf}
     * @return a newly created {@code SummaSearcher}
     * @throws Configurable.ConfigurationException if there is an error reading
     *                                             {@code conf} or there is an
     *                                             error creating the searcher
     *                                             instance
     */
    public static SummaSearcher createSearcher (Configuration conf,
                                                Class<? extends SummaSearcher> defaultClass) {
        log.trace("createSeacher called");

        Class<? extends SummaSearcher> seacherClass;
        try {
            seacherClass = conf.getClass(SummaSearcher.PROP_CLASS,
                                         SummaSearcher.class,
                                         defaultClass);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException ("Could not get searcher"
                                                           + " class from property "
                                                           + SummaSearcher.PROP_CLASS,
                                                           e);
        }
        
        log.debug("Instantiating searcher class: " + seacherClass);

        try {
            return Configuration.create(seacherClass, conf);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException ("Failed to instantiate"
                                                           + " seacher class "
                                                           + seacherClass, e);
        }
    }

    /**
     * Call {@link #createSearcher(Configuration, Class)} with the default class
     * set to {@link SummaSearcherImpl}.
     *
     * @param conf The configuration to use
     * @return newly instantiated searcher
     */
    public static SummaSearcher createSearcher (Configuration conf) {
        return createSearcher (conf, SummaSearcherImpl.class);
    }
}
