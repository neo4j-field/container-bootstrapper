package io.sisu.neo4j.server;

import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseInternalSettings;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.facade.GraphDatabaseDependencies;
import org.neo4j.io.IOUtils;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.scheduler.BufferingExecutor;
import org.neo4j.kernel.internal.Version;
import org.neo4j.logging.FormattedLogProvider;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.RotatingFileOutputStreamSupplier;
import org.neo4j.scheduler.Group;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.CommandLineArgs;
import org.neo4j.server.ServerStartupException;
import org.neo4j.server.logging.JULBridge;
import org.neo4j.server.logging.JettyLogBridge;
import sun.misc.Signal;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static org.neo4j.io.fs.FileSystemUtils.createOrOpenAsOutputStream;

public abstract class ContainerBootstrapper implements Bootstrapper {

    protected static String convertEnvToProp(String env) {
        return env
                .replaceAll("NEO4J_", "")
                .replaceAll("_", ".")
                .replaceAll("\\.\\.", "_");
    }

    public static int start(Bootstrapper boot, String... argv) {
        Map<String, String> config = new HashMap<>();
        CommandLineArgs args = CommandLineArgs.parse(argv);

        if (args.version()) {
            System.out.println("neo4j " + Version.getNeo4jVersion());
            return 0;
        }

        if (args.homeDir() == null) {
            throw new ServerStartupException("Argument --home-dir is required and was not provided.");
        }

        // XXX: We take our config overrides from the environment, just like the Docker entrypoint.sh
        System.getenv().forEach((key, val) -> {
            if (key.startsWith("NEO4J_")
                    && !key.equalsIgnoreCase("neo4j_home")
                    && !key.equalsIgnoreCase("neo4j_conf")
                    && !key.equalsIgnoreCase("NEO4J_ACCEPT_LICENSE_AGREEMENT")) {
                String property = convertEnvToProp(key);
                System.out.printf("Adding config override from environment %s=%s%n", property, val);
                config.put(property, val);
            }
        });

        return boot.start(args.homeDir(), args.configFile(), config);
    }

    @Override
    public int start(File homeDir, File configFile, Map<String, String> configOverrides) {
        // This is called by NeoBootstrapper and our primary mechanism for controlling the startup of Neo4j
        // Most of this is going to be copy-pasta from NeoBootstrapper.java
        addShutdownHook();
        installSignalHandlers();

        Config config = Config.newBuilder()
                .setDefaults(GraphDatabaseSettings.SERVER_DEFAULTS)
                .fromFileNoThrow(configFile)
                .setRaw(configOverrides)
                .set(GraphDatabaseSettings.neo4j_home, homeDir.toPath().toAbsolutePath())
                .build();

        try {
            LogProvider userLogProvider = setupLogging(config);
            dependencies = dependencies.userLogProvider(userLogProvider);
            log = userLogProvider.getLog(getClass());
            config.setLogger(log);

            serverAddress = HttpConnector.listen_address.toString();

            log.info("*** CONTAINER FRIENDLY NEO4J IS Starting...");
            databaseManagementService = createNeo(config, dependencies);
            log.info("*** CONTAINER FRIENDLY NEO4J IS Started.");

            return OK;
        } catch (ServerStartupException e) {
            e.describeTo(log);
            return WEB_SERVER_STARTUP_ERROR_CODE;
        } catch (TransactionFailureException tfe) {
            String locationMsg = (databaseManagementService == null) ? "" :
                    " Another process may be using databases at location: " + config.get(GraphDatabaseInternalSettings.databases_root_path);
            log.error(format("Failed to start Neo4j on %s.", serverAddress) + locationMsg, tfe);
            return GRAPH_DATABASE_STARTUP_ERROR_CODE;
        } catch (Exception e) {
            log.error(format("Failed to start Neo4j on %s.", serverAddress), e);
            return WEB_SERVER_STARTUP_ERROR_CODE;
        }
    }

    protected abstract DatabaseManagementService createNeo(Config config, GraphDatabaseDependencies dependencies);

    // ********************************************************************************************
    //   Copy-pasta from org/neo4j/server/NeoBootstrapper.java in Neo4j 4.1.1
    // ********************************************************************************************

    public static final int OK = 0;
    private static final int WEB_SERVER_STARTUP_ERROR_CODE = 1;
    private static final int GRAPH_DATABASE_STARTUP_ERROR_CODE = 2;
    private static final String SIGTERM = "TERM";
    private static final String SIGINT = "INT";

    private volatile DatabaseManagementService databaseManagementService;
    private volatile Closeable userLogFileStream;
    private Thread shutdownHook;
    private GraphDatabaseDependencies dependencies = GraphDatabaseDependencies.newDependencies();
    // in case we have errors loading/validating the configuration log to stdout
    private Log log = FormattedLogProvider.toOutputStream(System.out).getLog(getClass());
    private String serverAddress = "unknown address";

    @Override
    public int stop() {
        String location = "unknown location";
        try {
            doShutdown();

            removeShutdownHook();

            return 0;
        } catch (Exception e) {
            log.error("Failed to cleanly shutdown Neo Server on port [%s], database [%s]. Reason [%s] ",
                    serverAddress, location, e.getMessage(), e);
            return 1;
        }
    }

    public boolean isRunning() {
        return databaseManagementService != null;
    }

    public Log getLog() {
        return log;
    }

    private LogProvider setupLogging(Config config) {
        FormattedLogProvider.Builder builder = FormattedLogProvider
                .withoutRenderingContext()
                .withZoneId(config.get(GraphDatabaseSettings.db_timezone).getZoneId())
                .withDefaultLogLevel(config.get(GraphDatabaseSettings.store_internal_log_level))
                .withFormat(config.get(GraphDatabaseInternalSettings.log_format));

        LogProvider userLogProvider = config.get(GraphDatabaseSettings.store_user_log_to_stdout) ? builder.toOutputStream(System.out)
                : createFileSystemUserLogProvider(config, builder);

        JULBridge.resetJUL();
        Logger.getLogger("").setLevel(Level.WARNING);
        JULBridge.forwardTo(userLogProvider);
        JettyLogBridge.setLogProvider(userLogProvider);
        return userLogProvider;
    }

    // Exit gracefully if possible
    private static void installSignalHandlers() {
        installSignalHandler(SIGTERM, false); // SIGTERM is invoked when system service is stopped
        installSignalHandler(SIGINT, true); // SIGINT is invoked when user hits ctrl-c  when running `neo4j console`
    }

    private static void installSignalHandler(String sig, boolean tolerateErrors) {
        try {
            // System.exit() will trigger the shutdown hook
            Signal.handle(new Signal(sig), signal -> System.exit(0));
        } catch (Throwable e) {
            if (!tolerateErrors) {
                throw e;
            }
            // Errors occur on IBM JDK with IllegalArgumentException: Signal already used by VM: INT
            // I can't find anywhere where we send a SIGINT to neo4j process so I don't think this is that important
        }
    }

    private void doShutdown() {
        if (databaseManagementService != null) {
            log.info("Stopping...");
            databaseManagementService.shutdown();
            log.info("Stopped.");
        }
        if (userLogFileStream != null) {
            closeUserLogFileStream();
        }
    }

    private void closeUserLogFileStream() {
        IOUtils.closeAllUnchecked(userLogFileStream);
    }

    private void addShutdownHook() {
        shutdownHook = new Thread(() -> {
            log.info("Neo4j Server shutdown initiated by request");
            doShutdown();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void removeShutdownHook() {
        if (shutdownHook != null) {
            if (!Runtime.getRuntime().removeShutdownHook(shutdownHook)) {
                log.warn("Unable to remove shutdown hook");
            }
        }
    }

    private LogProvider createFileSystemUserLogProvider(Config config, FormattedLogProvider.Builder builder) {
        BufferingExecutor deferredExecutor = new BufferingExecutor();
        dependencies = dependencies.withDeferredExecutor(deferredExecutor, Group.LOG_ROTATION);

        FileSystemAbstraction fs = new DefaultFileSystemAbstraction();
        File destination = config.get(GraphDatabaseSettings.store_user_log_path).toFile();
        Long rotationThreshold = config.get(GraphDatabaseSettings.store_user_log_rotation_threshold);
        try {
            if (rotationThreshold == 0L) {
                OutputStream userLog = createOrOpenAsOutputStream(fs, destination, true);
                // Assign it to the server instance so that it gets closed when the server closes
                this.userLogFileStream = userLog;
                return builder.toOutputStream(userLog);
            }
            RotatingFileOutputStreamSupplier rotatingUserLogSupplier = new RotatingFileOutputStreamSupplier(fs, destination, rotationThreshold,
                    config.get(GraphDatabaseSettings.store_user_log_rotation_delay).toMillis(),
                    config.get(GraphDatabaseSettings.store_user_log_max_archives), deferredExecutor);
            // Assign it to the server instance so that it gets closed when the server closes
            this.userLogFileStream = rotatingUserLogSupplier;
            return builder.toOutputStream(rotatingUserLogSupplier);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
