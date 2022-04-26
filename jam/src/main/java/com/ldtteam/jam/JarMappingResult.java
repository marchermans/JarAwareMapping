package com.ldtteam.jam;

import com.ldtteam.jam.spi.mapping.MappingResult;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public record JarMappingResult(MappingResult<ClassNode> classes, MappingResult<MethodNode> methods, MappingResult<FieldNode> fields)
{
}
