package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;

public record MethodData<TClassPayload, TMethodPayload>(ClassData<TClassPayload> owner, MethodNode node, Optional<TMethodPayload> payload) {
}
