package com.ldtteam.jam.jamspec.meta;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.Set;

public interface IASMData
{
    Set<ClassNode> classes();

    Set<MethodNode> methods();

    Set<FieldNode> fields();

    Set<ParameterNode> parameters();
}
