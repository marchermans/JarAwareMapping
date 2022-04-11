package com.ldtteam.jam.jamspec.name;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Function;

@FunctionalInterface
public interface INameProvider<T> extends Function<T, String>
{
    static INameProvider<ClassNode> classes() {
        return node -> node.name;
    }

    static INameProvider<MethodNode> methods() {
        return node -> node.name + node.desc;
    }

    static INameProvider<FieldNode> fields() {
        return node -> node.name;
    }

    String getName(T t);

    @Override
    default String apply(T t) {
        return getName(t);
    }
}
