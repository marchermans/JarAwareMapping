package com.ldtteam.jam.neoform;

import com.ldtteam.jam.spi.ast.named.INamedAST;
import com.ldtteam.jam.spi.configuration.MetadataWritingConfiguration;
import com.ldtteam.jam.spi.writer.INamedASTOutputWriter;
import com.machinezoo.noexception.Exceptions;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.nio.file.Path;

public class TSRGNamedASTWriter implements INamedASTOutputWriter {

    private static final String JOINED_TSRG_FILE_NAME = "joined.tsrg";

    public static INamedASTOutputWriter create() {
        return new TSRGNamedASTWriter();
    }

    private TSRGNamedASTWriter() {
    }

    @Override
    public void write(
            final Path outputDirectory,
            final MetadataWritingConfiguration metadataWritingConfiguration,
            final INamedAST ast) {
        IMappingBuilder builder = IMappingBuilder.create("obf", "srg", "id");

        ast.classes().forEach(namedClass -> {
            final IMappingBuilder.IClass classMapping = builder.addClass(
                    namedClass.originalName(),
                    namedClass.identifiedName(),
                    String.valueOf(namedClass.id())
            );

            namedClass.fields().forEach(namedField -> classMapping.field(
                    namedField.originalName(),
                    namedField.identifiedName(),
                    String.valueOf(namedField.id())
            ));

            namedClass.methods().forEach(namedMethod -> {
                final IMappingBuilder.IMethod methodMapping = classMapping.method(
                        namedMethod.originalDescriptor(),
                        namedMethod.originalName(),
                        namedMethod.identifiedName(),
                        String.valueOf(namedMethod.id())
                );

                if (namedMethod.isStatic()) {
                    methodMapping.meta("is_static", "true");
                }

                if (metadataWritingConfiguration.writeLambdaMetaInformationValue()) {
                    if (namedMethod.isLambda()) {
                        methodMapping.meta("is_lambda", "true");
                    }
                }

                namedMethod.parameters().forEach(namedParameter -> methodMapping.parameter(
                        namedParameter.index(),
                        namedParameter.originalName(),
                        namedParameter.identifiedName(),
                        String.valueOf(namedParameter.id())
                ));
            });
        });

        final INamedMappingFile mappingFile = builder.build();
        Exceptions.sneak().run(
                () -> mappingFile.write(
                        outputDirectory.resolve(JOINED_TSRG_FILE_NAME),
                        IMappingFile.Format.TSRG2
                )
        );
    }
}
