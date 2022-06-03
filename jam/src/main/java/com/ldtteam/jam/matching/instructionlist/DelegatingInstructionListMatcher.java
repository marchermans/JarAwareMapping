package com.ldtteam.jam.matching.instructionlist;

import com.ldtteam.jam.spi.matching.IMatcher;
import com.ldtteam.jam.spi.matching.MatchingResult;
import org.objectweb.asm.tree.InsnList;

import java.util.Arrays;
import java.util.List;

public class DelegatingInstructionListMatcher<TType> implements IMatcher<TType> {

    @SafeVarargs
    public static <TMatchType> IMatcher<TMatchType> create(final IMatcher<TMatchType>... matchers) {
        return new DelegatingInstructionListMatcher<>(Arrays.asList(matchers));
    }

    private final List<IMatcher<TType>> matchers;

    private DelegatingInstructionListMatcher(List<IMatcher<TType>> matchers) {
        this.matchers = matchers;
    }

    @Override
    public MatchingResult match(TType left, TType right) {
        return matchers.stream()
                .map(matcher -> matcher.match(left, right))
                .filter(result -> result != MatchingResult.UNKNOWN)
                .findFirst()
                .orElse(MatchingResult.FAIL);
    }
}
