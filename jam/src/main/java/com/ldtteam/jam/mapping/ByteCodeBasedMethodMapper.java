package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.InsnList;

import java.util.Optional;
import java.util.Set;

public final class ByteCodeBasedMethodMapper extends SingleEntryBasedMapper<MethodData>
{

    private final IMatcher<InsnList> matcher;

    public static IMapper<MethodData> create(final IMatcher<InsnList> matcher) {
        return new ByteCodeBasedMethodMapper(matcher);
    }

    private ByteCodeBasedMethodMapper(IMatcher<InsnList> matcher)
    {
        this.matcher = matcher;
    }

    @Override
    public Optional<MethodData> map(final MethodData source, final Set<MethodData> candidates)
    {
        return candidates.stream().filter(candidate -> matcher.match(source.node().instructions, candidate.node().instructions) == MatchingResult.MATCH).findFirst();
    }
}
