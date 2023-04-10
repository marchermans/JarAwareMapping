package com.ldtteam.jam.loader;

import com.ldtteam.jam.spi.asm.*;
import com.ldtteam.jam.spi.payload.IPayloadSupplier;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Optional;
import java.util.Set;

public record LoadedASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>(String name, Set<ClassData<TClassPayload>> classes, Set<MethodData<TClassPayload, TMethodPayload>> methods, Set<FieldData<TClassPayload, TFieldPayload>> fields, Set<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameters, Optional<IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>> payloadSupplier) implements IASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload>
{
}
