package com.ldtteam.jammer.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class FilterUtils
{

    private FilterUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: FilterUtils. This is a utility class");
    }

    public static boolean isClassFile(final Path path) {
        return Files.isRegularFile(path) && path.toString().endsWith(".class");
    }
}
