package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.Strings;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class DeployCommand extends Command {

    private ConnectionManager<ScoreConnection> cm;
    private String scoreAddress;

    public DeployCommand(ConnectionManager<ScoreConnection> cm,
                         String scoreAddress) {
        super("deploy", "Deploy a client bundle");

        setUsage("deploy [options] <bundle-id> <instance-id> <target-host>");

        installOption ("t", "transport", true, "Which deployment transport to"
                                             + " use. Allowed values are 'ssh'."
                                             + "Default is ssh");

        installOption ("b", "basepath", true, "What basepath to use for the client"
                                            + " installation relative to the "
                                            + "client user's home directory. "
                                            + "Default is 'summa-score'");

        installOption ("c", "configuration", true, "Url, RMI address or file"
                                                 + " path where the client can"
                                                 + " find its configuration."
                                                 + " Default points at the "
                                                 + "Score configuration server");

        this.cm = cm;
        this.scoreAddress = scoreAddress;

    }

    public String getDeployerClassName (String shortDesc) {
        if ("ssh".equals (shortDesc)) {
            return "dk.statsbiblioteket.summa.score.server.deploy.SSHDeployer";
        } else {
            throw new BadConfigurationException ("Unknown deployment transport "
                                               + "'" + shortDesc + "'");
        }
    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Extract and validate arguments */
        String[] args = getArguments();
        if (args.length != 3) {
            ctx.error("You must provide exactly 3 arguments. Found " + args.length);
            return;
        }
        String bundleId = args[0];
        String instanceId = args[1];
        String target = args[2];

        String transport = getOption("t") != null ? getOption("t") : "ssh";
        transport = getDeployerClassName(transport);

        String basePath = getOption("b") != null ? getOption("b") : "summa-score";
        String confLocation = getOption("c"); // This is allowed to be unset
                                              // - see ClientDeployer#CLIENT_CONF_PROPERTY

        /* Set up a configuration for the deployment request */
        Configuration conf =
                Configuration.newMemoryBased(ClientDeployer.BASEPATH_PROPERTY,
                                             basePath,
                                             ClientDeployer.CLIENT_CONF_PROPERTY,
                                             confLocation,
                                             ClientDeployer.DEPLOYER_BUNDLE_PROPERTY,
                                             bundleId,
                                             ClientDeployer.INSTANCE_ID_PROPERTY,
                                             instanceId,
                                             ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                                             transport,
                                             ClientDeployer.DEPLOYER_TARGET_PROPERTY,
                                             target);

        /* Connect to the Score and send the deployment request */
        ctx.prompt ("Deploying '" + instanceId + "' on '" + target + "' using "
                    + "'" + transport + "' transport... ");
        ConnectionContext<ScoreConnection> connCtx = null;
        try {
            connCtx = cm.get (scoreAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Score server at '"
                           + scoreAddress + "'");
                return;
            }

            ScoreConnection score = connCtx.getConnection();
            score.deployClient(conf);

        } catch (Exception e) {
            ctx.error (Strings.getStackTrace(e));
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }

        ctx.info ("OK");


    }
}
