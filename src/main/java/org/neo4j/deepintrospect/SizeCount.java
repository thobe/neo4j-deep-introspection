package org.neo4j.deepintrospect;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.neo4j.kernel.impl.util.ArrayMap;

public class SizeCount
{
    private final long totSize;
    private final long totCount;
    private final long tagSize;
    private final long tagCount;
    private final long objCount;
    private final List<String> objectSizes = new ArrayList<String>();

    SizeCount( long totSize, long totCount, long tagSize, long tagCount, long objCount )
    {
        this.totSize = totSize;
        this.totCount = totCount;
        this.tagSize = tagSize;
        this.tagCount = tagCount;
        this.objCount = objCount;
    }

    void specSize( Object obj, long size, long count )
    {
        Class<?> type = obj.getClass();
        StringBuilder repr;
        if ( type.isArray() )
        {
            int depth;
            for ( depth = 0; type.isArray(); depth++ )
                type = type.getComponentType();
            repr = new StringBuilder( type.isPrimitive() ? type.getSimpleName() : type.getName() );
            for ( ; depth > 1; depth-- )
                repr.append( "[]" );
            repr.append( "[" );
            repr.append( Array.getLength( obj ) );
            repr.append( "]" );
        }
        else
        {
            repr = new StringBuilder( obj.getClass().getName() );
            if ( obj instanceof String )
            {
                repr.append( " length=" );
                repr.append( Integer.toString( ( (String) obj ).length() ) );
            }
            else if ( obj instanceof Collection<?> )
            {
                repr.append( " size=" );
                repr.append( Integer.toString( ( (Collection<?>) obj ).size() ) );
            }
            else if ( obj instanceof Map<?, ?> )
            {
                repr.append( " size=" );
                repr.append( Integer.toString( ( (Map<?, ?>) obj ).size() ) );
            }
            else if ( obj instanceof ArrayMap<?, ?> )
            {
                repr.append( " size=" );
                repr.append( Integer.toString( ( (ArrayMap<?, ?>) obj ).size() ) );
            }
        }
        long objSize = InspectAgent.sizeOf( obj );
        if ( objSize == 0 )
        {
            repr.append( " [Object size unknown] " );
        }
        else
        {
            repr.append( " " );
            repr.append( format( objSize ) );
            if ( count != 0 ) repr.append( " + " );
        }
        if ( count != 0 )
        {
            repr.append( format( size ) );
            repr.append( " in " );
            repr.append( Long.toString( count ) );
            repr.append( " referenced objects" );
        }
        objectSizes.add( repr.toString() );
    }

    @Override
    public String toString()
    {
        StringBuilder result;
        if ( totCount == tagCount )
        {
            result = new StringBuilder( format( tagSize, tagCount, objCount ) );
        }
        else
        {
            result = new StringBuilder( String.format(
                    "\n        total: %s\n        payload: %s\n        overhead: %s",//
                    format( totSize, totCount ), format( tagSize, tagCount, objCount ),//
                    format( totSize - tagSize, totCount - tagCount ) ) );
        }
        if ( !objectSizes.isEmpty() )
        {
            result.append( "\n      Objects:" );
            for ( String size : objectSizes )
            {
                result.append( "\n        " + size );
            }
        }
        return result.toString();
    }

    private static String format( long size, long count, long elementCount )
    {
        return format( size ) + " (in " + count + " objects, " + elementCount + " elements)";
    }

    private static String format( long size, long count )
    {
        return format( size ) + " (in " + count + " objects)";
    }

    @SuppressWarnings( "boxing" )
    private static String format( long bytes )
    {
        if ( bytes < 10000 )
        {
            return bytes + "B";
        }
        else if ( bytes < 10000000 )
        {
            return String.format( "%.3fkB [%d]", ( (double) bytes ) / ( (double) ( 1024 ) ), bytes );
        }
        else
        {
            return String.format( "%.3fMB [%d]", ( (double) bytes ) / ( (double) ( 1024 * 1024 ) ),
                    bytes );
        }
    }
}
