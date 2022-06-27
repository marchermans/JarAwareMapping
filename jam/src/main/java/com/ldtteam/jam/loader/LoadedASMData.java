package com.ldtteam.jam.loader;

import com.ldtteam.jam.spi.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Set;

public record LoadedASMData(String name, Set<ClassData> classes, Set<MethodData> methods, Set<FieldData> fields, Set<ParameterData> parameters) implements IASMData
{
}
