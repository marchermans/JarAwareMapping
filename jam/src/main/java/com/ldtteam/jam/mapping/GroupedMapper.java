package com.ldtteam.jam.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Multimap;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.util.GroupingUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class GroupedMapper<T, G> implements IMapper<T>
{
    private final Function<T, G> groupSelector;
    private final Function<G, IMapper<T>> groupMapper;

    public GroupedMapper(final Function<T, G> groupSelector, final Function<G, IMapper<T>> groupMapper) {
        this.groupSelector = groupSelector;
        this.groupMapper = groupMapper;
    }

    @Override
    public MappingResult<T> map(final Set<T> sources, final Set<T> candidates)
    {
        final Multimap<G, T> sourceGroups = GroupingUtils.groupByUsingSet(sources, groupSelector);
        final Multimap<G, T> candidateGroups = GroupingUtils.groupByUsingSet(candidates, groupSelector);

        final Set<T> unmappedSources = new HashSet<>(sources);
        final Set<T> unmappedCandidates = new HashSet<>(candidates);
        final BiMap<T, T> mappings = HashBiMap.create(sources.size());

        for (final G group : sourceGroups.keySet()){
            if (candidateGroups.containsKey(group)){
                final IMapper<T> groupMapper = this.groupMapper.apply(group);
                final Set<T> groupSource = new HashSet<>(sourceGroups.get(group));
                final Set<T> groupCandidates = new HashSet<>(candidateGroups.get(group));

                final MappingResult<T> groupMapping = groupMapper.map(groupSource, groupCandidates);

                unmappedSources.removeAll(groupMapping.mappings().keySet());
                mappings.putAll(groupMapping.mappings());
                unmappedCandidates.removeAll(groupMapping.mappings().values());
            }
        }

        return new MappingResult<>(unmappedSources, mappings, unmappedCandidates);
    }
}
