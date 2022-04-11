package com.ldtteam.jam.jamspec.writer;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.jamspec.meta.IASMData;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.nio.file.Path;

public interface IOutputWriter
{

    void write(final Path outputDirectory, final BiMap<ClassNode, Integer> classIds, final BiMap<MethodNode, Integer> methodIds, final BiMap<FieldNode, Integer> fieldIds, final BiMap<ParameterNode, Integer> parameterIds, final
      IASMData asmData);
}
