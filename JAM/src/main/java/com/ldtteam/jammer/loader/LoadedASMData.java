package com.ldtteam.jammer.loader;

import com.ldtteam.jam.jamspec.meta.IASMData;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Set;

public record LoadedASMData(String name, Set<ClassNode> classes, Set<MethodNode> methods, Set<FieldNode> fields, Set<ParameterNode> parameters) implements IASMData
{
}
