package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.FieldNode;

import java.util.Optional;

public record FieldData<TClassPayload, TFieldPayload>(ClassData<TClassPayload> owner, FieldNode node, Optional<TFieldPayload> payload) {
}
