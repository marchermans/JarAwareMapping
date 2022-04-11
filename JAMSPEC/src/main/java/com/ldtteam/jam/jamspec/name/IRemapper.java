package com.ldtteam.jam.jamspec.name;

import java.util.Optional;

public interface IRemapper
{
    Optional<String> remapClass(final String className);

    Optional<String> remapMethod(final String className, final String name, final String descriptor);

    Optional<String> remapField(final String className, final String name, final String type);

    Optional<String> remapParameter(String className, String methodName, String descriptor, final String parameterName, int index);

    Optional<String> remapPackage(String packageName);
}
