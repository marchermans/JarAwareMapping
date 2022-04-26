package com.ldtteam.jam.spi.configuration;

import com.ldtteam.jam.spi.mapping.IMapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public record MappingRuntimeConfiguration(IMapper<ClassNode> classMapper, IMapper<MethodNode> methodMapper, IMapper<FieldNode> fieldMapper)
{
}
