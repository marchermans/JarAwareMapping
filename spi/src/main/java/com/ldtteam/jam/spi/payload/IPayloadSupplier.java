package com.ldtteam.jam.spi.payload;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Optional;

/**
 * Supplier for the payload context of existing data.
 *
 * @param <TClassPayload> The payload type for the class.
 * @param <TFieldPayload> The payload type for the field.
 * @param <TMethodPayload> The payload type for the method.
 * @param <TParameterPayload> The payload type for the parameter.
 */
public interface IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> {

    /**
     * Create an empty payload supplier.
     *
     * @param <TClassPayload> The payload type for the class.
     * @param <TFieldPayload> The payload type for the field.
     * @param <TMethodPayload> The payload type for the method.
     * @param <TParameterPayload> The payload type for the parameter.
     * @return The empty payload supplier.
     */
    static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> empty() {
        return new IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>() {
            @Override
            public Optional<TClassPayload> forClass(ClassNode classData) {
                return Optional.empty();
            }

            @Override
            public Optional<TMethodPayload> forMethod(ClassNode classNode, MethodNode methodData) {
                return Optional.empty();
            }

            @Override
            public Optional<TFieldPayload> forField(ClassNode classNode, FieldNode fieldData) {
                return Optional.empty();
            }

            @Override
            public Optional<TParameterPayload> forParameter(ClassNode classNode, MethodNode methodNode, ParameterNode parameterData) {
                return Optional.empty();
            }
        };
    }

    /**
     * Get the payload for the given class.
     *
     * @param classData The class data.
     * @return The payload.
     */
    Optional<TClassPayload> forClass(final ClassNode classData);

    /**
     * Get the payload for the given method.
     *
     * @param classNode The class node.
     * @param methodData The method data.
     * @return The payload.
     */
    Optional<TMethodPayload> forMethod(final ClassNode classNode, final MethodNode methodData);

    /**
     * Get the payload for the given field.
     *
     * @param classNode The class node.
     * @param fieldData The field data.
     * @return The payload.
     */
    Optional<TFieldPayload> forField(final ClassNode classNode, final FieldNode fieldData);

    /**
     * Get the payload for the given parameter.
     *
     * @param classNode The class node.
     * @param methodNode The method node.
     * @param parameterData The parameter data.
     * @return The payload.
     */
    Optional<TParameterPayload> forParameter(final ClassNode classNode, final MethodNode methodNode, final ParameterNode parameterData);
}
