package org.neo4j.deepintrospect;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import javax.management.NotCompliantMBeanException;

import org.neo4j.kernel.KernelExtension.KernelData;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.management.impl.ManagementBeanProvider;
import org.neo4j.management.impl.Neo4jMBean;

final class IntrospectorImpl extends Neo4jMBean implements Introspector
{
    private static final Field NODE_CACHE, REL_CACHE;
    private static final Class<?> NODE_IMPL, REL_IMPL;
    static
    {
        NODE_CACHE = nmField( "nodeCache" );
        REL_CACHE = nmField( "relCache" );
        NODE_IMPL = getClass( "org.neo4j.kernel.impl.core.NodeImpl" );
        REL_IMPL = getClass( "org.neo4j.kernel.impl.core.RelationshipImpl" );
    }

    private final Object nodeCache, relCache;

    IntrospectorImpl( ManagementBeanProvider provider, KernelData kernel )
            throws NotCompliantMBeanException
    {
        super( provider, kernel );
        NodeManager nm = kernel.getConfig().getGraphDbModule().getNodeManager();
        try
        {
            nodeCache = NODE_CACHE.get( nm );
            relCache = REL_CACHE.get( nm );
        }
        catch ( Exception e )
        {
            throw new UnsupportedOperationException( "Reflection failure", e );
        }
    }

    private static Class<?> getClass( String className )
    {
        try
        {
            return Class.forName( className );
        }
        catch ( ClassNotFoundException e )
        {
            throw new UnsupportedOperationException( "Reflection failure", e );
        }
    }

    private static Field nmField( String name )
    {
        try
        {
            Field field = NodeManager.class.getDeclaredField( name );
            field.setAccessible( true );
            return field;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return null;
        }
    }

    public static void premain( String agentArguments, Instrumentation instrumentation )
    {
        agentmain( agentArguments, instrumentation );
    }

    public static void agentmain( String agentArguments, Instrumentation instrumentation )
    {
        if ( instrumentation != null ) IntrospectorImpl.instrumentation = instrumentation;
    }

    private static volatile Instrumentation instrumentation;
    private static final ToolingInterface tooling = ToolingInterface.getInstance();

    public String getNodeCacheSize()
    {
        return nodeCacheSize( NODE_IMPL );
    }

    public String getRelCacheSize()
    {
        return relCacheSize( REL_IMPL );
    }

    public String computeNodeCacheSize( String className )
    {
        return nodeCacheSize( getClass( className ) );
    }

    public String computeRelCacheSize( String className )
    {
        return relCacheSize( getClass( className ) );
    }

    private String nodeCacheSize( Class<?> payloadLimit )
    {
        return tooling.getTransitiveSize( nodeCache, payloadLimit ).toString();
    }

    private String relCacheSize( Class<?> payloadLimit )
    {
        return tooling.getTransitiveSize( relCache, payloadLimit ).toString();
    }

    /*
    public String getObjectSize( Class<?> type )
    {
        tooling.getSizeOfAll(type);
        return "";
    }
    */
}
