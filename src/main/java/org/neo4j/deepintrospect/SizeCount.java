package org.neo4j.deepintrospect;

class SizeCount
{
    final long size;
    final long count;
    final long objSize;
    final long objCount;

    SizeCount( long size, long count, long objSize, long objCount )
    {
        this.size = size;
        this.count = count;
        this.objSize = objSize;
        this.objCount = objCount;
    }

    @Override
    public String toString()
    {
        return String.format( "total: %s, payload objects: %s, overhead: %s",
                format( size, count ), format( objSize, objCount ), format( size - objSize,
                        count - objCount ) );
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
