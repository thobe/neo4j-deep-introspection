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
    static
    {
        NODE_CACHE = nmField( "nodeCache" );
        REL_CACHE = nmField( "relCache" );
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
        return format( tooling.getTransitiveSize( nodeCache ) );
    }

    private String format( SizeCount sizeCount )
    {
        return format( sizeCount.size ) + " (in " + sizeCount.count + " objects)";
    }

    public String getRelCacheSize()
    {
        return format( tooling.getTransitiveSize( relCache ) );
    }

    private String format( long bytes )
    {
        if ( bytes < 10000 )
        {
            return bytes + "B";
        }
        else if ( bytes < 10000000 )
        {
            return String.format( "%.3fkB", ( (double) bytes ) / ( (double) ( 1024 ) ) );
        }
        else
        {
            return String.format( "%.3fMB", ( (double) bytes ) / ( (double) ( 1024 * 1024 ) ) );
        }
    }
}
