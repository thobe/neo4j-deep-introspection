package org.neo4j.deepintrospect;

public interface Introspector
{
    final String NAME = "Deep introspector";

    String getNodeCacheSize();

    String getRelCacheSize();

    String computeNodeCacheSize( String className );

    String computeRelCacheSize( String className );

    // String getObjectSize( Class<?> type );
}
