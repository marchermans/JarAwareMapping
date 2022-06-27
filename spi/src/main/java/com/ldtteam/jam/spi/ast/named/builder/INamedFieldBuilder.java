package com.ldtteam.jam.spi.ast.named.builder;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.named.INamedField;

public interface INamedFieldBuilder
{
    INamedField build(
      ClassData classData,
      FieldData fieldData,
      IMetadataClass classMetadata,
      BiMap<FieldData, FieldData> fieldMappings,
      BiMap<FieldData, Integer> fieldIds
    );
}
