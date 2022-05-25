package com.ldtteam.jam.spi.name;

import java.util.Optional;

public interface IRemapper
{

    static IRemapper identity() {
        return new IRemapper() {
            @Override
            public Optional<String> remapClass(String className) {
                return Optional.of(className);
            }

            @Override
            public Optional<String> remapMethod(String className, String name, String descriptor) {
                return Optional.of(name);
            }

            @Override
            public Optional<String> remapDescriptor(String descriptor) {
                return Optional.of(descriptor);
            }

            @Override
            public Optional<String> remapField(String className, String name, String type) {
                return Optional.of(name);
            }

            @Override
            public Optional<String> remapParameter(String className, String methodName, String descriptor, String parameterName, int index) {
                return Optional.of(parameterName);
            }

            @Override
            public Optional<String> remapPackage(String packageName) {
                return Optional.of(packageName);
            }
        };
    }

    Optional<String> remapClass(final String className);

    Optional<String> remapMethod(final String className, final String name, final String descriptor);

    Optional<String> remapDescriptor(final String descriptor);

    Optional<String> remapField(final String className, final String name, final String type);

    Optional<String> remapParameter(String className, String methodName, String descriptor, final String parameterName, int index);

    Optional<String> remapPackage(String packageName);
}
