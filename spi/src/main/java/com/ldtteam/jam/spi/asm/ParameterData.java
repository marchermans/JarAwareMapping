package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.ParameterNode;

import java.util.Optional;

public record ParameterData<TClassPayload, TMethodPayload, TParameterPayload>(ClassData<TClassPayload> classOwner, MethodData<TClassPayload, TMethodPayload> owner, ParameterNode node, int index, String desc, Optional<TParameterPayload> payload) {
}
