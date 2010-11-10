package org.neo4j.deepintrospect;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class TestInspector
{
    private static EmbeddedGraphDatabase graphDb;

    @BeforeClass
    public static void startGraphDb()
    {
        graphDb = new EmbeddedGraphDatabase( "target/data/" + TestInspector.class.getSimpleName() );
    }

    @AfterClass
    public static void stopGraphDb()
    {
        if ( graphDb != null ) graphDb.shutdown();
        graphDb = null;
    }

    @Test
    public void canGetNodeCacheSize() throws Exception
    {
        Introspector tool = graphDb.getManagementBean( Introspector.class );
        assertNotNull( "Could not access the Introspector bean", tool );
        System.out.println( "Node cache size: " + tool.getNodeCacheSize() );
        System.out.println( "Relationship cache size: " + tool.getRelCacheSize() );
        graphDb.getReferenceNode();
        System.out.println( "Node cache size: " + tool.getNodeCacheSize() );
    }
}
