package org.neo4j.deepintrospect;

import javax.management.NotCompliantMBeanException;

import org.neo4j.kernel.KernelExtension.KernelData;
import org.neo4j.management.impl.ManagementBeanProvider;
import org.neo4j.management.impl.Neo4jMBean;

public class IntrospectorBean extends ManagementBeanProvider
{
    public IntrospectorBean()
    {
        super( Introspector.class );
    }

    @Override
    protected Neo4jMBean createMBean( KernelData kernel ) throws NotCompliantMBeanException
    {
        try
        {
            return new IntrospectorImpl( this, kernel );
        }
        catch ( RuntimeException e )
        {
            e.printStackTrace();
            throw e;
        }
        catch ( Error e )
        {
            e.printStackTrace();
            throw e;
        }
    }
}
