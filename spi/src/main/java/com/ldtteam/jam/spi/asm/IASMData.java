package com.ldtteam.jam.spi.asm;

import com.ldtteam.jam.spi.payload.IPayloadSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Optional;
import java.util.Set;

/**
 * Represents the data from the jar in ASM format.
 */
public interface IASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{
    /**
     * Gives access to the class information in the jar.
     *
     * @return The classes in the jar.
     */
    @NonNull
    Set<ClassData<TClassPayload>> classes();

    /**
     * Gives access to the method information in the jar.
     *
     * @return The methods in the jar.
     */
    @NonNull
    Set<MethodData<TClassPayload, TMethodPayload>> methods();

    /**
     * Gives access to the field information in the jar.
     *
     * @return The fields in the jar.
     */
    @NonNull
    Set<FieldData<TClassPayload, TFieldPayload>> fields();

    /**
     * Gives access to the parameter information in the jar.
     *
     * @return The parameters in the jar.
     */
    @NonNull
    Set<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameters();

    /**
     * Gives access to the payload supplier.
     *
     * @return The payload supplier.
     */
    @NonNull
    Optional<IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>> payloadSupplier();
}
