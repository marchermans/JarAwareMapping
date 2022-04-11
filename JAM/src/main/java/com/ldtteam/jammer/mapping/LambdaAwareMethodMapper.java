package com.ldtteam.jammer.mapping;

import com.ldtteam.jam.jamspec.mapping.IMapper;
import org.objectweb.asm.tree.MethodNode;

public final class LambdaAwareMethodMapper extends GroupedMapper<MethodNode, LambdaAwareMethodMapper.MethodType>
{
    public static IMapper<MethodNode> create(
      final IMapper<MethodNode> noneLambdaMapper,
      final IMapper<MethodNode> lambdaMapper
    ) {
        return new LambdaAwareMethodMapper(noneLambdaMapper, lambdaMapper);
    }

    private LambdaAwareMethodMapper(final IMapper<MethodNode> noneLambdaMapper, final IMapper<MethodNode> lambdaMapper) {
        super(
          methodNode -> methodNode.name.contains("lambda$") ? MethodType.LAMBDA : MethodType.NONE_LAMBDA,
          methodType -> methodType == MethodType.LAMBDA ? lambdaMapper : noneLambdaMapper
        );
    }

    enum MethodType {
        LAMBDA,
        NONE_LAMBDA
    }
}
