package com.ldtteam.jam.statistics;

import com.ldtteam.jam.spi.statistics.IMappingStatistics;

public final class MappingStatistics implements IMappingStatistics
{
    private final TypeMappingStatistics directClassStatistics  = new TypeMappingStatistics();
    private final TypeMappingStatistics directMethodStatistics = new TypeMappingStatistics();
    private final TypeMappingStatistics directFieldStatistics  = new TypeMappingStatistics();

    private final TypeMappingStatistics rejuvenatedClassStatistics  = new TypeMappingStatistics();
    private final TypeMappingStatistics rejuvenatedMethodStatistics = new TypeMappingStatistics();
    private final TypeMappingStatistics rejuvenatedFieldStatistics  = new TypeMappingStatistics();

    private final TypeMappingStatistics renamedClassStatistics  = new TypeMappingStatistics();
    private final TypeMappingStatistics renamedMethodStatistics = new TypeMappingStatistics();
    private final TypeMappingStatistics renamedFieldStatistics  = new TypeMappingStatistics();

    private final TypeMappingStatistics totalClassStatistics  = new TypeMappingStatistics();
    private final TypeMappingStatistics totalMethodStatistics = new TypeMappingStatistics();
    private final TypeMappingStatistics totalFieldStatistics  = new TypeMappingStatistics();

    @Override
    public TypeMappingStatistics getDirectClassStatistics()
    {
        return directClassStatistics;
    }

    @Override
    public TypeMappingStatistics getDirectMethodStatistics()
    {
        return directMethodStatistics;
    }

    @Override
    public TypeMappingStatistics getDirectFieldStatistics()
    {
        return directFieldStatistics;
    }

    @Override
    public TypeMappingStatistics getRejuvenatedClassStatistics()
    {
        return rejuvenatedClassStatistics;
    }

    @Override
    public TypeMappingStatistics getRejuvenatedMethodStatistics()
    {
        return rejuvenatedMethodStatistics;
    }

    @Override
    public TypeMappingStatistics getRejuvenatedFieldStatistics()
    {
        return rejuvenatedFieldStatistics;
    }

    @Override
    public TypeMappingStatistics getRenamedClassStatistics()
    {
        return renamedClassStatistics;
    }

    @Override
    public TypeMappingStatistics getRenamedMethodStatistics()
    {
        return renamedMethodStatistics;
    }

    @Override
    public TypeMappingStatistics getRenamedFieldStatistics()
    {
        return renamedFieldStatistics;
    }

    @Override
    public TypeMappingStatistics getTotalClassStatistics()
    {
        return totalClassStatistics;
    }

    @Override
    public TypeMappingStatistics getTotalMethodStatistics()
    {
        return totalMethodStatistics;
    }

    @Override
    public TypeMappingStatistics getTotalFieldStatistics()
    {
        return totalFieldStatistics;
    }
}
