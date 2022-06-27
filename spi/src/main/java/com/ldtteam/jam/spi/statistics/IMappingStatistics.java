package com.ldtteam.jam.spi.statistics;

public interface IMappingStatistics
{

    ITypedMappingStatistics getDirectClassStatistics();

    ITypedMappingStatistics getDirectMethodStatistics();

    ITypedMappingStatistics getDirectFieldStatistics();

    ITypedMappingStatistics getDirectParameterStatistics();

    ITypedMappingStatistics getRejuvenatedClassStatistics();

    ITypedMappingStatistics getRejuvenatedMethodStatistics();

    ITypedMappingStatistics getRejuvenatedFieldStatistics();

    ITypedMappingStatistics getRejuvenatedParameterStatistics();

    ITypedMappingStatistics getRenamedClassStatistics();

    ITypedMappingStatistics getRenamedMethodStatistics();

    ITypedMappingStatistics getRenamedFieldStatistics();

    ITypedMappingStatistics getRenamedParameterStatistics();

    ITypedMappingStatistics getTotalClassStatistics();

    ITypedMappingStatistics getTotalMethodStatistics();

    ITypedMappingStatistics getTotalFieldStatistics();

    ITypedMappingStatistics getTotalParameterStatistics();
}
