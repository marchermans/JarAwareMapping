package com.ldtteam.jam.spi.name;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;

import java.util.function.Function;

@FunctionalInterface
public interface INotObfuscatedFilter<TData>
{
    static INotObfuscatedFilter<ClassData> notObfuscatedClassIfAnnotatedBy(String... annotations) {
        return data -> {
            for (String annotation : annotations) {
                if (data.node().visibleAnnotations != null) {
                    for (var annotationNode : data.node().visibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }

                if (data.node().invisibleAnnotations != null) {
                    for (var annotationNode : data.node().invisibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    static INotObfuscatedFilter<MethodData> notObfuscatedMethodIfAnnotatedBy(String... annotations) {
        final INotObfuscatedFilter<ClassData> classFilter = notObfuscatedClassIfAnnotatedBy(annotations);
        return data -> {
            if (classFilter.isNotObfuscated(data.owner())) {
                return true;
            }

            for (String annotation : annotations) {
                if (data.node().visibleAnnotations != null) {
                    for (var annotationNode : data.node().visibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }

                if (data.node().invisibleAnnotations != null) {
                    for (var annotationNode : data.node().invisibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    static INotObfuscatedFilter<FieldData> notObfuscatedFieldIfAnnotatedBy(String... annotations) {
        final INotObfuscatedFilter<ClassData> classFilter = notObfuscatedClassIfAnnotatedBy(annotations);
        return data -> {
            if (classFilter.isNotObfuscated(data.owner())) {
                return true;
            }

            for (String annotation : annotations) {
                if (data.node().visibleAnnotations != null) {
                    for (var annotationNode : data.node().visibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }

                if (data.node().invisibleAnnotations != null) {
                    for (var annotationNode : data.node().invisibleAnnotations) {
                        if (annotationNode.desc.equals(annotation)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        };
    }

    static INotObfuscatedFilter<ParameterData> notObfuscatedParameterIfAnnotatedBy(String... annotations) {
        final INotObfuscatedFilter<MethodData> methodFilter = notObfuscatedMethodIfAnnotatedBy(annotations);
        return data -> methodFilter.isNotObfuscated(data.owner());
    }

    boolean isNotObfuscated(TData data);
}
