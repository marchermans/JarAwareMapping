package com.ldtteam.jam.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.spi.name.INameProvider;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

public class AlignedMapper<N> implements IMapper<N>
{

    public static IMapper<ClassNode> classes(final IMapper<ClassNode> inner) {
        return create(
          INameProvider.classes(),
          inner
        );
    }

    public static IMapper<MethodNode> methods(final IMapper<MethodNode> inner) {
        return create(
          INameProvider.methods(),
          inner
        );
    }

    public static IMapper<FieldNode> fields(final IMapper<FieldNode> inner) {
        return create(
          INameProvider.fields(),
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

        final Set<N> unmappedSources = new HashSet<>();
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

        return new MappingResult<>(unmappedSources, mappings, new HashSet<>(sortedCandidates));
    }
}
