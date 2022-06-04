package com.ldtteam.jam.spi.statistics;

public interface IMappingStatistics
{

    ITypedMappingStatistics getDirectClassStatistics();

    ITypedMappingStatistics getDirectMethodStatistics();

    ITypedMappingStatistics getDirectFieldStatistics();

    ITypedMappingStatistics getRejuvenatedClassStatistics();

    ITypedMappingStatistics getRejuvenatedMethodStatistics();

    ITypedMappingStatistics getRejuvenatedFieldStatistics();

    ITypedMappingStatistics getRenamedClassStatistics();

    ITypedMappingStatistics getRenamedMethodStatistics();

    ITypedMappingStatistics getRenamedFieldStatistics();

    ITypedMappingStatistics getTotalClassStatistics();

    ITypedMappingStatistics getTotalMethodStatistics();

    ITypedMappingStatistics getTotalFieldStatistics();
}
