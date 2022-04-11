package com.ldtteam.jam.jamspec.identification;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

public interface INewIdentitySupplier
{
    int getClassIdentity(final ClassNode classNode);

    int getMethodIdentity(final ClassNode classNode, final MethodNode methodNode);

    int getFieldIdentity(final ClassNode classNode, final FieldNode fieldNode);

    int getParameterIdentity(final ClassNode classNode, final MethodNode methodNode, final ParameterNode parameterNode, final int index);
}
