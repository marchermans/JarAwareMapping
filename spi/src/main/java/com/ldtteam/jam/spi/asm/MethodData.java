package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.MethodNode;

public record MethodData(ClassData owner, MethodNode node) {
}
