package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.IMapper;

public final class LambdaAwareMethodMapper extends GroupedMapper<MethodData, LambdaAwareMethodMapper.MethodType>
{
    public static IMapper<MethodData> create(
      final IMapper<MethodData> noneLambdaMapper,
      final IMapper<MethodData> lambdaMapper
    ) {
        return new LambdaAwareMethodMapper(noneLambdaMapper, lambdaMapper);
    }

    private LambdaAwareMethodMapper(final IMapper<MethodData> noneLambdaMapper, final IMapper<MethodData> lambdaMapper) {
        super(
          methodData -> methodData.node().name.contains("lambda$") ? MethodType.LAMBDA : MethodType.NONE_LAMBDA,
          methodType -> methodType == MethodType.LAMBDA ? lambdaMapper : noneLambdaMapper
        );
    }

    enum MethodType {
        LAMBDA,
        NONE_LAMBDA
    }
}
