package org.neo4j.deepintrospect;

public interface Introspector
{
    final String NAME = "Deep introspector";

    String getNodeCacheSize();

    String getRelCacheSize();
}
