package org.neo4j.deepintrospect;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

@SuppressWarnings( "restriction" )
class InspectAgent
{
    public static void premain( String agentArguments, Instrumentation instrumentation )
    {
        agentmain( agentArguments, instrumentation );
    }

    public static void agentmain( String agentArguments, Instrumentation instrumentation )
    {
        if ( instrumentation != null ) InspectAgent.instrumentation = instrumentation;
    }

    private static volatile Instrumentation instrumentation;

    static long sizeOf( Object obj )
    {
        Instrumentation inst = instrumentation;
        return inst == null ? 0 : inst.getObjectSize( obj );
    }

    private static final sun.misc.Unsafe unsafe;
    static
    {
        sun.misc.Unsafe theUnsafe = null;
        try
        {
            theUnsafe = sun.misc.Unsafe.getUnsafe();
        }
        catch ( Throwable retry )
        {
            try
            {
                Field UNSAFE = sun.misc.Unsafe.class.getDeclaredField( "theUnsafe" );
                UNSAFE.setAccessible( true );
                theUnsafe = (sun.misc.Unsafe) UNSAFE.get( null );
            }
            catch ( Throwable fail )
            {
            }
        }
        finally
        {
            unsafe = theUnsafe;
        }
    }

    static long offset( Field field )
    {
        if ( unsafe == null ) return -1;
        return unsafe.objectFieldOffset( field );
    }
}
