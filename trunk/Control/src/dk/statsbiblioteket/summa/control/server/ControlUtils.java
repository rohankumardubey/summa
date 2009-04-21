package dk.statsbiblioteket.summa.control.server;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.control.api.bundle.BundleRepository;
import dk.statsbiblioteket.summa.control.api.BadConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Various utils for general use in the {@link ControlCore} and related
 * classes.
 */
public class ControlUtils {

    private static final Log log = LogFactory.getLog (ControlUtils.class);

    /**
     * <p>Work out what base dir to use given a configuration. This is
     * extracted from the {@link ControlCore#CONF_CONTROL_BASE_DIR} property.</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return base directory for installation
     */
    public static File getControlBaseDir (Configuration conf) {
        String basePath = conf.getString (ControlCore.CONF_CONTROL_BASE_DIR,
                                          System.getProperty("user.home")
                                        + File.separator
                                        + "summa-control");
        File baseDir = new File (basePath);
        if (!baseDir.exists()) {
            if (baseDir.mkdirs()) {
                log.debug("Created control base directory '" + baseDir + "'");
            }
        }

        log.trace ("Calculated control base dir '" + baseDir + "'");
        return baseDir;
    }    

    /**
     * <p>Get the directory used to store incoming bundles uploaded by the
     * control administrator(s).</p>
     *
     * <p>If the directory does not exist it will be created.</p>
     *
     * @param conf configuration to extract the properties from
     * @return file pointing at the directory for incoming bundles
     */
    public static File getIncomingDir (Configuration conf) {
        String incPath = conf.getString (RepositoryManager.CONF_INCOMING_DIR,
                                         new File(getControlBaseDir(conf),
                                                   "incoming").toString());

        File incDir = new File (incPath);
        if (!incDir.exists()) {
            if (incDir.mkdirs()) {
                log.debug ("Created dir for incoming bundles '" + incDir + "'");
            }
        }
        return incDir;
    }

    /**
     * Get the address the repository is exposed on. This is the address
     * clients will use to download bundles from.
     * @param conf
     * @return
     */
    public static String getRepositoryAddress (Configuration conf) {
        String address =
                  conf.getString(BundleRepository.CONF_REPO_ADDRESS,
                                 "http://"
                                 + RemoteHelper.getHostname() + ":8080"
                                 + "/summa-control/repo");
        return address;
    }

    /**
     * Translate abbreviated deployer class name to fully qualified class name
     * @param shortDesc deployer abbrev.
     * @return the fulle qualified class name
     * @throws BadConfigurationException if the deployer is not known
     */
    public static String getDeployerClassName (String shortDesc) {
        if ("ssh".equals (shortDesc)) {
            return "dk.statsbiblioteket.summa.control.server.deploy.SSHDeployer";
        } else {
            throw new BadConfigurationException("Unknown deployment transport "
                                               + "'" + shortDesc + "'");
        }
    }

    /**
     * Read the (unzipped) contents of a single zip entry within a zip file.
     * <p></p>
     * TODO: Move this method to Zips in sbutil
     * @param zipFile zip file to read from
     * @param entryName name of entry withing the zip file
     * @return a byte array with the unpacked data, or null if the entry is
     *         not found within the zip file
     * @throws IOException if there is an error reading the zip file
     */
    public static byte[] getZipEntry (File zipFile, String entryName)
            throws IOException {
        ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        byte[] buf = new byte[2048];
        int count = 0;


        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().equals(entryName)) {
                while ((count = zip.read(buf, 0, buf.length)) != -1) {
                    out.write(buf, 0, count);
                }
                return out.toByteArray();
            } else {
                zip.closeEntry();
            }
        }
        return null;
    }
}



