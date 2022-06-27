package com.ldtteam.jam.spi.asm;

import org.objectweb.asm.tree.ParameterNode;

public record ParameterData(ClassData classOwner, MethodData owner, ParameterNode node, int index, String desc) {
}
