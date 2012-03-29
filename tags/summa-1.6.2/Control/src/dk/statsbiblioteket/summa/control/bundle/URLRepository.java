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
package dk.statsbiblioteket.summa.control.bundle;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A {@link dk.statsbiblioteket.summa.control.api.bundle.BundleRepository}
 * fetching bundles via Java {@link URL}s.
 * <p/>
 * This class can support listing of repository contents in two ways. The
 * first is local-only and works on all {@code file://} URLs. The second
 * requires the server to list all bundles in a file called {@code bundles.list}
 * in the base URL of the repository.
 *
 * <p>Given a bundle id it is mapped to a URL as specified by the
 * {@link #CONF_REPO_ADDRESS} property in the {@link Configuration}.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class URLRepository implements BundleRepository {
    private static final long serialVersionUID = 8978138452681L;
    private String tmpDir;
    private Log log = LogFactory.getLog(this.getClass());

    /**
     * Set from the {@link #CONF_BASE_URL} property
     */
    protected String baseUrl;

    /**
     * Set from the {@link #CONF_API_BASE_URL}
     */
    protected String apiBaseUrl;

    /**
     * Configuration property defining the base URL from bundles are
     * downloaded. Consumers will download the bundle files from
     * {@code <baseUrl>/<bundleId>.bundle}.
     * <p></p>
     * If this property is unset the value {@link #CONF_REPO_ADDRESS} of
     * will be used. If this property again is unset, the fallback value will be
     * {@link #DEFAULT_REPO_URL}
     */
    public static final String CONF_BASE_URL =
                                         "summa.control.repository.baseurl";

    /**
     * Configuration property defining the base URL from which jar files are
     * served. Consumers will download the jar files from
     * {@code <baseUrl>/<jarFileName>}. This controls the behavior of
     * {@link #expandApiUrl}.
     * <p></p>
     * If this property is unset {@link #baseUrl}{@code /api}
     * will be used as fallback.
     */
    public static final String CONF_API_BASE_URL =
                                         "summa.control.repository.api.baseurl";

    /**
     * Fallback value for the {@link #CONF_BASE_URL} used if the
     * initial fallback property {@link #CONF_REPO_ADDRESS} is also unset.
     */
    public static final String DEFAULT_REPO_URL = "file://"
                                                  + System.getProperty("user.home")
                                                  + "/summa-control"
                                                  + "/repository";

    /**
     * <p>Create a new URLRepository. If the {@link #CONF_DOWNLOAD_DIR} is
     * not set {@code tmp/} relative to the working directory will be used.</p>
     *
     * <p>If {@link #CONF_REPO_ADDRESS} is not set in the configuration
     * <code>file://${user.home}/summa-control/repo</code> is used.</p>
     * @param conf  The configuration.
     */
    public URLRepository (Configuration conf) {
        this.tmpDir = conf.getString(CONF_DOWNLOAD_DIR, "tmp");

        String repoAddress = conf.getString (BundleRepository.CONF_REPO_ADDRESS,
                                             DEFAULT_REPO_URL);
        this.baseUrl = conf.getString (CONF_BASE_URL,
                                       repoAddress);

        /* make sure baseUrl ends with a slash */
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        this.apiBaseUrl = conf.getString (URLRepository.CONF_API_BASE_URL,
                                          baseUrl + "api");

        /* make sure apiBaseUrl ends with a slash */
        if (!apiBaseUrl.endsWith("/")) {
            apiBaseUrl += "/";
        }

        log.debug ("Created " + this.getClass().getName()
                 + " instance with base URL " + baseUrl + " and API URL "
                 + apiBaseUrl);
    }

    public File get(String bundleId) throws IOException {
        log.trace ("Getting '" + bundleId + " from URLRepository");
        URL bundleUrl = new URL (baseUrl + bundleId + ".bundle");
        return download(bundleUrl);

    }

    private File download (URL url) throws IOException {
        log.trace("Preparing to download " + url);
        String filename = url.getPath();
        filename = filename.substring (filename.lastIndexOf("/") + 1);
        File result = new File (tmpDir, filename);

        /* Ensure tmpDir exists */
        if(!new File (tmpDir).mkdirs()) {
            log.warn("Directory '" + tmpDir + "' wasn't created");
        }

        /* make sure we find an unused filename to store the downloaded pkg in */
        int count = 0;
        while (result.exists()) {
            log.trace ("Target file " + result + " already exists");
            result = new File (tmpDir, filename + "." + count);
            count++;
        }

        log.debug ("Downloading " + url + " to " + result);
        InputStream con = new BufferedInputStream(url.openStream());
        OutputStream out = new BufferedOutputStream(
                                                 new FileOutputStream (result));

        Streams.pipe (con, out);

        log.debug ("Done downloading " + url + " to " + result);

        return result;

    }

    /**
     * List the contents of the repository. This always works for local
     * {@code file://} repositories, but for remote repositories it requires
     * the existence of a {@code bundles.list} file with the repo contents.
     *
     * @param regex regex returned bundles must match
     * @return a list of bundles matching {@code regex}
     * @throws IOException
     */
    public List<String> list (String regex) throws IOException {
        log.trace ("Got list() request for '" + regex + "'");

        if (baseUrl.startsWith("file://")) {
            return listDir(regex);
        } else {
            return listUrl(regex);
        }
    }

    public List<String> listDir (String regex) throws IOException {
        File baseDir = Resolver.urlToFile(new URL(baseUrl));
        Pattern pat = Pattern.compile(regex);
        List<String> result = new ArrayList <String> (10);

        for (String bdl : baseDir.list()) {
            if (!bdl.endsWith(".bundle")) {
                log.trace ("Skipping non-.bundle '" + bdl + "'");
                continue;
            }
            bdl = bdl.replace (".bundle", "");
            if (pat.matcher(bdl).matches()) {
                result.add (bdl);
                log.trace ("Match: " + bdl);
            } else {
                log.trace ("No match: " + bdl);
            }
        }

        return result;
    }

    public List<String> listUrl (String regex) throws IOException {
        log.debug("Downloading bundle list: " + baseUrl + "bundles.list");
        URL url = new URL(baseUrl + "bundles.list");
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        Streams.pipe(url.openStream(), out);
        String bundleList = new String(out.toByteArray());
        String[] bundles = bundleList.split("\\s");

        Pattern pat = Pattern.compile(regex);
        List<String> result = new ArrayList <String> (10);

        for (String bdl : bundles) {
            if (!bdl.endsWith(".bundle")) {
                log.trace ("Skipping non-.bundle '" + bdl + "'");
                continue;
            }
            bdl = bdl.replace (".bundle", "");
            if (pat.matcher(bdl).matches()) {
                result.add (bdl);
                log.trace ("Match: " + bdl);
            } else {
                log.trace ("No match: " + bdl);
            }
        }

        return result;
    }

    public String expandApiUrl (String jarFileName) throws IOException {
        // By contract we must return URLs as is
        if (jarFileName.startsWith("http://") ||
            jarFileName.startsWith("https://")||
            jarFileName.startsWith("ftp://")  ||
            jarFileName.startsWith("sftp://")) {
            log.trace ("Returning AIP URL of jar file as is: " + jarFileName);
            return jarFileName;
        }

        /* Only take the basename into account */
        jarFileName = new File (jarFileName).getName();

        String result = apiBaseUrl + jarFileName;
        log.trace ("API URL of " + jarFileName + " is: " + result);
        return result;
    }
}



