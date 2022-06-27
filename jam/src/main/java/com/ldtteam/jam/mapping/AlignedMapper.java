package com.ldtteam.jam.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.util.SetsUtil;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class AlignedMapper<N> implements IMapper<N>
{

    public static IMapper<ClassData> classes(final IMapper<ClassData> inner) {
        return create(
          INameProvider.classes(),
          inner
        );
    }

    public static IMapper<MethodData> methods(final IMapper<MethodData> inner) {
        return create(
          INameProvider.methods(),
          inner
        );
    }

    public static IMapper<FieldData> fields(final IMapper<FieldData> inner) {
        return create(
          INameProvider.fields(),
          inner
        );
    }

    public static IMapper<ParameterData> parameters(final IMapper<ParameterData> inner) {
        return create(
          Comparator.comparing(ParameterData::index),
          inner
        );
    }

    private static <E> IMapper<E> create(
      final INameProvider<E> nameProvider,
      final IMapper<E> inner
    )
    {
        return create(
          Comparator.comparing(nameProvider),
          inner
        );
    }

    public static <E> IMapper<E> create(
      final Comparator<E> sorter,
      final IMapper<E> inner)
    {
        return new AlignedMapper<>(sorter, inner);
    }

    private final Comparator<N> sorter;
    private final IMapper<N> inner;

    private AlignedMapper(final Comparator<N> sorter, final IMapper<N> inner) {
        this.sorter = sorter;
        this.inner = inner;
    }

    @Override
    public MappingResult<N> map(final Set<N> sources, final Set<N> candidates)
    {
        final SortedSet<N> sortedSources = new TreeSet<>(sorter);
        sortedSources.addAll(sources);

        final SortedSet<N> sortedCandidates = new TreeSet<>(sorter);
        sortedCandidates.addAll(candidates);

        final Set<N> unmappedSources = Sets.newHashSet();
        final BiMap<N, N> mappings = HashBiMap.create();

        while(!sortedSources.isEmpty()) {
            final N source = sortedSources.first();
            sortedSources.remove(source);

            final Set<N> sourceOnlySet = ImmutableSet.of(source);
            final MappingResult<N> innerResult = inner.map(sourceOnlySet, sortedCandidates);

            sortedCandidates.clear();
            sortedCandidates.addAll(innerResult.unmappedCandidates());

            mappings.putAll(innerResult.mappings());

            unmappedSources.addAll(innerResult.unmappedSources());
        }

        return new MappingResult<>(unmappedSources, mappings, SetsUtil.cloneSet(unmappedSources));
    }
}
