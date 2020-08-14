package io.sisu.neo4j;

import org.neo4j.dbms.DatabaseStateService;
import org.neo4j.dbms.DefaultDatabaseStateService;
import org.neo4j.dbms.database.DatabaseManager;
import org.neo4j.dbms.database.DefaultDatabaseManager;
import org.neo4j.dbms.database.StandaloneDatabaseContext;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.context.EditionDatabaseComponents;
import org.neo4j.graphdb.factory.module.edition.context.StandaloneDatabaseComponents;
import org.neo4j.graphdb.factory.module.id.IdContextFactory;
import org.neo4j.kernel.database.NamedDatabaseId;
import org.neo4j.kernel.impl.api.CommitProcessFactory;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.locking.StatementLocksFactory;
import org.neo4j.token.TokenHolders;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Straight copy-pasta of {@link org.neo4j.graphdb.factory.module.edition.StandaloneEditionModule}
 */
public abstract class ContainerEditionModule extends AbstractEditionModule  {
    protected CommitProcessFactory commitProcessFactory;
    protected DatabaseStateService databaseStateService;
    IdContextFactory idContextFactory;
    Function<NamedDatabaseId, TokenHolders> tokenHoldersProvider;
    Supplier<Locks> locksSupplier;
    Function<Locks, StatementLocksFactory> statementLocksFactoryProvider;

    @Override
    public EditionDatabaseComponents createDatabaseComponents(NamedDatabaseId namedDatabaseId )
    {
        return new ContainerDatabaseComponents( this, namedDatabaseId );
    }

    public CommitProcessFactory getCommitProcessFactory()
    {
        return commitProcessFactory;
    }

    public IdContextFactory getIdContextFactory()
    {
        return idContextFactory;
    }

    public Function<NamedDatabaseId,TokenHolders> getTokenHoldersProvider()
    {
        return tokenHoldersProvider;
    }

    public Supplier<Locks> getLocksSupplier()
    {
        return locksSupplier;
    }

    public Function<Locks,StatementLocksFactory> getStatementLocksFactoryProvider()
    {
        return statementLocksFactoryProvider;
    }

    @Override
    public DatabaseManager<StandaloneDatabaseContext> createDatabaseManager(GlobalModule globalModule )
    {
        var databaseManager = new DefaultDatabaseManager( globalModule, this );
        databaseStateService = new DefaultDatabaseStateService( databaseManager );

        globalModule.getGlobalLife().add( databaseManager );
        globalModule.getGlobalDependencies().satisfyDependency( databaseManager );
        globalModule.getGlobalDependencies().satisfyDependency( databaseStateService );

        return databaseManager;
    }
}
