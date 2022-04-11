package com.ldtteam.jammer.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ldtteam.jam.jamspec.mapping.IMapper;
import com.ldtteam.jam.jamspec.mapping.MappingResult;

import java.util.*;

public final class PhasedMapper<T> implements IMapper<T>
{

    @SafeVarargs
    public static <E> IMapper<E> create(final IMapper<E>... phases) {
        return new PhasedMapper<>(phases);
    }

    private final Queue<IMapper<T>> phases;

    @SafeVarargs
    private PhasedMapper(IMapper<T>... phases)
    {
        this.phases = new LinkedList<>();
        Collections.addAll(this.phases, phases);
    }


    @Override
    public MappingResult<T> map(final Set<T> sources, final Set<T> candidates)
    {
        final Set<T> unmappedSources = new HashSet<>(sources);
        final Set<T> unmappedCandidates = new HashSet<>(candidates);
        final BiMap<T, T> mappings = HashBiMap.create(sources.size());

        for (final IMapper<T> phase : phases) {
            final Set<T> phaseUnmappedSources = new HashSet<>(unmappedSources);
            final Set<T> phaseUnmappedCandidates = new HashSet<>(unmappedCandidates);

            final MappingResult<T> phaseResult = phase.map(phaseUnmappedSources, phaseUnmappedCandidates);

            unmappedSources.clear();
            unmappedCandidates.clear();

            unmappedSources.addAll(phaseResult.unmappedSources());
            unmappedCandidates.addAll(phaseResult.unmappedCandidates());

            mappings.putAll(phaseResult.mappings());
        }

        return new MappingResult<>(unmappedSources, mappings, unmappedCandidates);
    }
}
