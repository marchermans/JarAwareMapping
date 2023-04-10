package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.ClassNode;

import java.util.Optional;

public record ClassData<TClassPayload>(ClassNode node, Optional<TClassPayload> payload) {
}
