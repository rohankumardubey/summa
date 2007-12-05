/* $Id: XStorage.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: XStorage.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.configuration.storage;

import java.io.Serializable;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Class and some methods needs Javadoc")
public class XStorage extends XProperties implements ConfigurationStorage {
    public static final String DEFAULT_RESOURCE = "xconfiguration.xml";

    /**
     * Creates a XStorage around a XProperties.
     * @param properties the properties to wrap around.
     */
    protected XStorage(XProperties properties) {
        assignFrom(properties);
    }

    public XStorage() throws IOException {
        this(nextAvailableConfigurationFile());
    }

    public XStorage(Configuration configuration) throws IOException {
        this();
        new Configuration(this).importConfiguration(configuration);
    }

    public XStorage(File configurationFile) throws IOException {
        if (! configurationFile.exists()) {
            new XProperties().store(configurationFile.getAbsolutePath());
        } else {
            load(configurationFile.getAbsoluteFile().toString(),
                 false, false);
        }
    }

    public void put(String key, Serializable value) {
        super.put(key, value);
    }

    public Serializable get(String key) {
        return (Serializable)getObject(key);
    }

    public Iterator<Map.Entry<String, Serializable>> iterator() throws
                                                                IOException {
        log.warn("Iterators are not fully supported by XStorage. "
                 + "This won't work well with nesting");
        Map<String, Serializable> tempMap =
                new HashMap<String, Serializable>(size());
            for (Map.Entry<Object, Object> entry: entrySet()) {
                tempMap.put((String)entry.getKey(),
                            (Serializable)entry.getValue());
            }
        log.trace("Created shallow copy of storage with " + size()
                  + " elements. Returning iterator");
        return tempMap.entrySet().iterator();
    }

    public void purge(String key) {
        remove(key);
    }

    public int size() {
        return super.size();
    }

    public boolean supportsSubStorage() {
        return true;
    }

    public ConfigurationStorage getSubStorage(String key) throws IOException {
        Object sub = get(key);
        if (!(sub instanceof XProperties)) {
            throw new IOException("The value for '" + key + "' was of class '"
                                  + sub.getClass() + "'. Expected XStorage");
        }
        
        return new XStorage((XProperties)sub);
    }

    public ConfigurationStorage createSubStorage(
            String key) throws IOException {
        put(key, new XProperties());
        return getSubStorage(key);
    }

    private static File nextAvailableConfigurationFile () throws IOException {
        final String XCONFIGURATION = "xconfiguration.";
        int count = 0;
        File f = new File (XCONFIGURATION + count +".xml");
        while (f.exists()) {
            count++;
            f = new File (XCONFIGURATION + count +".xml");
        }
        return f;
    }
}
