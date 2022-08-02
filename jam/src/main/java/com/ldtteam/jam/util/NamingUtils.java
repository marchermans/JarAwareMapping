package com.ldtteam.jam.util;

public class NamingUtils
{

    private NamingUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: NamingUtils. This is a utility class");
    }

    public static String getOuterClassName(final String originalClassName)
    {
        if (!originalClassName.contains("$"))
        {
            return originalClassName;
        }

        final int lastIndexOfDollar = originalClassName.lastIndexOf("$");
        return originalClassName.substring(0, lastIndexOfDollar);
    }
}
