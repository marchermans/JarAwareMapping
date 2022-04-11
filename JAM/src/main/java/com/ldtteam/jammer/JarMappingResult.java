package com.ldtteam.jammer;

import com.ldtteam.jam.jamspec.mapping.MappingResult;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public record JarMappingResult(MappingResult<ClassNode> classes, MappingResult<MethodNode> methods, MappingResult<FieldNode> fields)
{
}
