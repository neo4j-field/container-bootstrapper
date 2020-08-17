package io.sisu.neo4j.server;

import org.neo4j.configuration.Config;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.facade.DatabaseManagementServiceFactory;
import org.neo4j.graphdb.facade.GraphDatabaseDependencies;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;

import static org.neo4j.kernel.impl.factory.DbmsInfo.COMMUNITY;

/**
 * Pretty much copy-pasta from {@link org.neo4j.server.CommunityBootstrapper}
 */
public class CommunityContainerBootstrapper extends ContainerBootstrapper {
    @Override
    protected DatabaseManagementService createNeo(Config config, GraphDatabaseDependencies dependencies) {
        DatabaseManagementServiceFactory facadeFactory = new DatabaseManagementServiceFactory(COMMUNITY, CommunityEditionModule::new);
        return facadeFactory.build(config, dependencies);
    }
}
