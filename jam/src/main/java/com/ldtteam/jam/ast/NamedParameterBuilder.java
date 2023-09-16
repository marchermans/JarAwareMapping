package com.ldtteam.jam.ast;

import com.google.common.collect.BiMap;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.ast.metadata.IMetadataRecordComponent;
import com.ldtteam.jam.spi.ast.named.INamedParameter;
import com.ldtteam.jam.spi.ast.named.builder.INamedParameterBuilder;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class NamedParameterBuilder implements INamedParameterBuilder
{
    public record ParameterNamingInformation(ParameterData target, ParameterData mappedFrom, Integer id) {}

    private static final Logger LOGGER = LoggerFactory.getLogger(NamedParameterBuilder.class);

    public static INamedParameterBuilder create(
          final IRemapper runtimeToASTRemapper,
          final IRemapper astToRuntimeRemapper,
      final INameProvider<ParameterNamingInformation> parameterIdToNameProvider
    )
    {
        return new NamedParameterBuilder(runtimeToASTRemapper, astToRuntimeRemapper, parameterIdToNameProvider);
    }

    private final IRemapper              runtimeToASTRemapper;
    private final IRemapper              astToRuntimeRemapper;
    private final INameProvider<ParameterNamingInformation> parameterNameProvider;

    private NamedParameterBuilder(IRemapper runtimeToASTRemapper, IRemapper astToRuntimeRemapper, INameProvider<ParameterNamingInformation> parameterNameProvider)
    {
        this.runtimeToASTRemapper = runtimeToASTRemapper;
        this.astToRuntimeRemapper = astToRuntimeRemapper;
        this.parameterNameProvider = parameterNameProvider;
    }

    @Override
    public INamedParameter build(
      final ClassData classData,
      final MethodData methodData,
      final ParameterData parameterData,
      final int index,
      final IMetadataClass classMetadata,
      final Map<String, String> identifiedFieldNamesByASTName,
      final BiMap<ParameterData, ParameterData> parameterMappings,
      final BiMap<ParameterData, Integer> parameterIds
    )
    {
        final int parameterId = parameterIds.get(parameterData);
        final String obfuscatedParameterName = runtimeToASTRemapper.remapParameter(
          classData.node().name,
          methodData.node().name,
          methodData.node().desc,
          parameterData.node().name,
          index
        ).orElseThrow(() -> new IllegalStateException("Missing the parameter mapping."));

        String mappedParameterName = null;


        if (methodData.node().name.equals("<init>") &&
              classMetadata.getMethodsByName() != null &&
              classMetadata.getRecords() != null &&
              !classMetadata.getRecords().isEmpty() &&
              methodData.node().parameters.size() == classMetadata.getRecords().size() &&
              isMatchingConstructor(parameterData, classMetadata, index)
        )
        {
            final IMetadataRecordComponent recordInfo = classMetadata.getRecords().get(index);
            if (identifiedFieldNamesByASTName.containsKey(recordInfo.getField()))
            {
                mappedParameterName = this.astToRuntimeRemapper.remapField(
                      this.runtimeToASTRemapper.remapClass(methodData.owner().node().name).orElseThrow(),
                      recordInfo.getField(),
                      recordInfo.getDesc()
                ).orElseThrow();
                LOGGER.debug("Remapped parameter of %s records constructor for id: %d to: %s".formatted(classData.node().name, parameterId, mappedParameterName));
            }
        }

        if (mappedParameterName == null)
        {
            final ParameterNamingInformation parameterNamingInformation = new ParameterNamingInformation(
                    parameterData,
                    parameterMappings.get(parameterData),
                    parameterId
            );
            mappedParameterName = parameterNameProvider.getName(parameterNamingInformation);
        }

        return new NamedParameter(
          obfuscatedParameterName,
          mappedParameterName,
          parameterId,
          index
        );
    }

    private record NamedParameter(String originalName, String identifiedName, int id, int index) implements INamedParameter {}

    private boolean isMatchingConstructor(final ParameterData constructorCandidate, IMetadataClass classMetadata, int index) {
        if (constructorCandidate.owner().node().name.equals("<init>")) {
            if (classMetadata.getMethodsByName() != null &&
                classMetadata.getRecords() != null &&
                !classMetadata.getRecords().isEmpty() &&
                constructorCandidate.owner().node().parameters.size() == classMetadata.getRecords().size()
            ) {
                final Optional<String> caniconicalConstructor = runtimeToASTRemapper.remapDescriptor(
                        constructorCandidate.owner().node().desc
                );
                final String caniconalConstructorCandidate = "(" + classMetadata.getRecords().stream().map(IMetadataRecordComponent::getDesc).collect(Collectors.joining()) + ")V";

                return caniconicalConstructor
                        .map(obfDescriptor -> obfDescriptor.equals(caniconalConstructorCandidate))
                        .orElse(false);
            }
        }

        return false;
    }
}
