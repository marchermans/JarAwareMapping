package com.ldtteam.jam.spi.asm;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Set;

/**
 * Represents the data from the jar in ASM format.
 */
public interface IASMData
{
    /**
     * Gives access to the class information in the jar.
     *
     * @return The classes in the jar.
     */
    @NonNull
    Set<ClassData> classes();

    /**
     * Gives access to the method information in the jar.
     *
     * @return The methods in the jar.
     */
    @NonNull
    Set<MethodData> methods();

    /**
     * Gives access to the field information in the jar.
     *
     * @return The fields in the jar.
     */
    Set<FieldData> fields();

    /**
     * Gives access to the parameter information in the jar.
     *
     * @return The parameters in the jar.
     */
    Set<ParameterData> parameters();
}
