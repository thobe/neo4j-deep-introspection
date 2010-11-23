package org.neo4j.deepintrospect;

import java.lang.reflect.Field;

public class Main
{
    private static class OneLong
    {
        long value;
    }

    private static class FourShorts
    {
        short value1, value2, value3, value4;
    }

    private static class OneIntAndTwoShorts
    {
        int value;
        short value1, value2;
    }

    private static class BaseInt
    {
        int a;
    }

    private static class InheritedInt extends BaseInt
    {
        int b;
    }

    private static class PackedInt
    {
        int a, b;
    }

    private static class Ordered
    {
        int a;
        int b;
        Object l;
    }

    private static class Unordered
    {
        int a;
        Object l;
        int x;
    }

    public static void main( String[] args )
    {
        equals( new OneLong(), new FourShorts() );
        equals( new OneLong(), new OneIntAndTwoShorts() );
        equals( new InheritedInt(), new PackedInt() );
        equals( new Ordered(), new Unordered() );
        System.out.println( "Size of Empty string: "
                            + ToolingInterface.instance().getTransitiveSize( "", String.class ) );
        for ( Class<?> type : Main.class.getDeclaredClasses() )
        {
            fieldOffsets( type );
        }
    }

    private static void fieldOffsets( Class<?> type )
    {
        ToolingInterface tool = ToolingInterface.instance();
        System.out.println( "Field offsets in " + type.getSimpleName() + ":" );
        for ( Field field : type.getDeclaredFields() )
        {
            System.out.println( "  " + field.getName() + ": " + tool.getOffset( field ) );
        }
    }

    private static void equals( Object one, Object two )
    {
        ToolingInterface tool = ToolingInterface.instance();
        long oneSize = tool.sizeOf( one ), twoSize = tool.sizeOf( two );
        System.out.printf( "%s: %d %s= %s: %d\n", one.getClass().getSimpleName(), oneSize,
                ( oneSize == twoSize ? '=' : '!' ), two.getClass().getSimpleName(), twoSize );
    }
}
