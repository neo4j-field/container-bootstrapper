package io.sisu.neo4j;

import org.neo4j.graphdb.factory.module.edition.StandaloneEditionModule;
import org.neo4j.graphdb.factory.module.edition.context.EditionDatabaseComponents;
import org.neo4j.graphdb.factory.module.id.DatabaseIdContext;
import org.neo4j.io.fs.watcher.DatabaseLayoutWatcher;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.pagecache.IOLimiter;
import org.neo4j.kernel.database.DatabaseStartupController;
import org.neo4j.kernel.database.NamedDatabaseId;
import org.neo4j.kernel.impl.api.CommitProcessFactory;
import org.neo4j.kernel.impl.constraints.ConstraintSemantics;
import org.neo4j.kernel.impl.factory.AccessCapabilityFactory;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.locking.StatementLocksFactory;
import org.neo4j.kernel.impl.query.QueryEngineProvider;
import org.neo4j.kernel.impl.transaction.stats.DatabaseTransactionStats;
import org.neo4j.token.TokenHolders;

import java.util.function.Function;

/**
 * Copy-Pasta mostly of {@link org.neo4j.graphdb.factory.module.edition.context.StandaloneDatabaseComponents}
 */
public class ContainerDatabaseComponents implements EditionDatabaseComponents {
    private final Function<DatabaseLayout, DatabaseLayoutWatcher> watcherServiceFactory;
    private final IOLimiter ioLimiter;
    private final ConstraintSemantics constraintSemantics;
    private final CommitProcessFactory commitProcessFactory;
    private final TokenHolders tokenHolders;
    private final Locks locks;
    private final DatabaseTransactionStats transactionMonitor;
    private final DatabaseIdContext idContext;
    private final StatementLocksFactory statementLocksFactory;
    private final QueryEngineProvider queryEngineProvider;
    private final AccessCapabilityFactory accessCapabilityFactory;
    private final DatabaseStartupController startupController;

    public ContainerDatabaseComponents(ContainerEditionModule editionModule, NamedDatabaseId namedDatabaseId )
    {
        this.commitProcessFactory = editionModule.getCommitProcessFactory();
        this.constraintSemantics = editionModule.getConstraintSemantics();
        this.ioLimiter = editionModule.getIoLimiter();
        this.watcherServiceFactory = editionModule.getWatcherServiceFactory();
        this.idContext = editionModule.getIdContextFactory().createIdContext( namedDatabaseId );
        this.tokenHolders = editionModule.getTokenHoldersProvider().apply( namedDatabaseId );
        this.locks = editionModule.getLocksSupplier().get();
        this.statementLocksFactory = editionModule.getStatementLocksFactoryProvider().apply( locks );
        this.transactionMonitor = editionModule.createTransactionMonitor();
        this.queryEngineProvider = editionModule.getQueryEngineProvider();
        this.startupController = editionModule.getDatabaseStartupController();
        this.accessCapabilityFactory = AccessCapabilityFactory.configDependent();
    }

    @Override
    public DatabaseIdContext getIdContext()
    {
        return idContext;
    }

    @Override
    public TokenHolders getTokenHolders()
    {
        return tokenHolders;
    }

    @Override
    public Function<DatabaseLayout,DatabaseLayoutWatcher> getWatcherServiceFactory()
    {
        return watcherServiceFactory;
    }

    @Override
    public IOLimiter getIoLimiter()
    {
        return ioLimiter;
    }

    @Override
    public ConstraintSemantics getConstraintSemantics()
    {
        return constraintSemantics;
    }

    @Override
    public CommitProcessFactory getCommitProcessFactory()
    {
        return commitProcessFactory;
    }

    @Override
    public Locks getLocks()
    {
        return locks;
    }

    @Override
    public StatementLocksFactory getStatementLocksFactory()
    {
        return statementLocksFactory;
    }

    @Override
    public DatabaseTransactionStats getTransactionMonitor()
    {
        return transactionMonitor;
    }

    @Override
    public QueryEngineProvider getQueryEngineProvider()
    {
        return queryEngineProvider;
    }

    @Override
    public AccessCapabilityFactory getAccessCapabilityFactory()
    {
        return accessCapabilityFactory;
    }

    @Override
    public DatabaseStartupController getStartupController()
    {
        return startupController;
    }
}
