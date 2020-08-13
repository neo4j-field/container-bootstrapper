package io.sisu.neo4j;

import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;

public class CommunityContainerEditionModule extends CommunityEditionModule {

    public CommunityContainerEditionModule(GlobalModule globalModule) {
        // TODO: Our SSL policy stuff is trapped in here! We need to let it out!
        super(globalModule);
    }
}
