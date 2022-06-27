package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.FieldNode;

public record FieldData(ClassData owner, FieldNode node) {
}
