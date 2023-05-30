package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.InsnList;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ByteCodeBasedMethodMapper extends SingleEntryBasedMapper<MethodData>
{

    private final IMatcher<InsnList> matcher;
    private final boolean terminateSearch;

    public static IMapper<MethodData> terminating(final IMatcher<InsnList> matcher) {
        return new ByteCodeBasedMethodMapper(matcher, true);
    }

    public static IMapper<MethodData> exact(final IMatcher<InsnList> matcher) {
        return new ByteCodeBasedMethodMapper(matcher, false);
    }

    private static IMapper<MethodData> create(final IMatcher<InsnList> matcher, final boolean terminateSearch) {
        return new ByteCodeBasedMethodMapper(matcher, terminateSearch);
    }

    private ByteCodeBasedMethodMapper(IMatcher<InsnList> matcher, boolean terminateSearch)
    {
        this.matcher = matcher;
        this.terminateSearch = terminateSearch;
    }

    @Override
    public Optional<MethodData> map(final MethodData source, final Set<MethodData> candidates)
    {
        final List<MethodData> potentialMatches = candidates.stream()
                .filter(candidate -> matcher.match(source.node().instructions, candidate.node().instructions) == MatchingResult.MATCH)
                .toList();

        //If we are forced to function as a terminus then we select the first random match returned by the matching candidates.
        if (terminateSearch && potentialMatches.size() > 0) {
            return Optional.of(potentialMatches.get(0));
        }

        //We are not a terminus but have an exact result.
        //We will use that exact result.
        if (potentialMatches.size() == 1) {
            return Optional.of(potentialMatches.get(0));
        }

        //We are a not terminus with multiple results, or have no results at all.
        return Optional.empty();
    }
}
