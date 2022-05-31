package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.*;

import java.util.Optional;
import java.util.Set;

public final class ByteCodeBasedMethodMapper extends SingleEntryBasedMapper<MethodNode>
{

    private final IMatcher matcher;

    public static IMapper<MethodNode> create(final IMatcher matcher) {
        return new ByteCodeBasedMethodMapper(matcher);
    }

    private ByteCodeBasedMethodMapper(IMatcher matcher)
    {
        this.matcher = matcher;
    }

    @Override
    public Optional<MethodNode> map(final MethodNode source, final Set<MethodNode> candidates)
    {
        return candidates.stream().filter(candidate -> matcher.match(source.instructions, candidate.instructions) == MatchingResult.MATCH).findFirst();
    }
}
