package org.neo4j.deepintrospect;

final class ToolingInterface
{
    static ToolingInterface getInstance()
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

    native SizeCount getTransitiveSize( Object obj, Class<?> type );

    // native SizeCount getSizeOfAll( Class<?> type );

    private ToolingInterface()
    {
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
