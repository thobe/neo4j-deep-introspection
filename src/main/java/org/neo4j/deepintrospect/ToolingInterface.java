package org.neo4j.deepintrospect;

import java.lang.reflect.Field;

public final class ToolingInterface
{
    public static ToolingInterface instance()
    {
        if ( isAvailable )
        {
            return new ToolingInterface();
        }
        else
        {
            throw new UnsupportedOperationException( "ToolingInterface not loaded." );
        }
    }

    public native SizeCount getTransitiveSize( Object obj, Class<?> type );

    private ToolingInterface()
    {
    }

    public long sizeOf( Object obj )
    {
        return InspectAgent.sizeOf( obj );
    }

    long getOffset( Field field )
    {
        return InspectAgent.offset( field );
    }

    private static native boolean setupNative( String options );

    private static final String NATIVE_LIBRARY_NAME = "deep-java-introspection";
    private static final boolean isAvailable;
    static
    {
        try
        {
            System.loadLibrary( NATIVE_LIBRARY_NAME );
        }
        catch ( Throwable t )
        {
        }
        isAvailable = setupNative( null );
    }
}
