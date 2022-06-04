package com.ldtteam.jam.statistics;

import com.ldtteam.jam.spi.mapping.MappingResult;
import com.ldtteam.jam.spi.statistics.ITypedMappingStatistics;

public class TypeMappingStatistics implements ITypedMappingStatistics
{
    int lost = 0;
    int mapped   = 0;
    int found = 0;

    public void loadFromMappingResult(final MappingResult<?> classes)
    {
        lost = classes.unmappedCandidates().size();
        mapped = classes.mappings().size();
        found = classes.unmappedSources().size();
    }

    public void load(final int lost, final int mapped, final int found)
    {
        this.lost = lost;
        this.mapped = mapped;
        this.found = found;
    }

    @Override
    public int getLost()
    {
        return lost;
    }

    @Override
    public int getMapped()
    {
        return mapped;
    }

    @Override
    public int getFound()
    {
        return found;
    }
}
