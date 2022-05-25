package com.ldtteam.jam.mcpconfig;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.metadata.IMetadataProvider;
import com.machinezoo.noexception.Exceptions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public class TSRGMetadataProvider implements IMetadataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TSRGMetadataProvider.class);

    public static IMetadataProvider create(final Path metadataFile) {
        return new TSRGMetadataProvider(metadataFile);
    }

    private static final Gson GSON                      = new GsonBuilder().create();
    private static final Type TYPE_MAP_STRING_CLASSINFO = new TypeToken<Map<String, MappingToyMetadata.ClassInfo>>() {}.getType();

    private final Path metadataFile;
    private final Supplier<IMetadataAST> metadataASTSupplier;

    private TSRGMetadataProvider(final Path metadataFile) {
        this.metadataFile = metadataFile;
        metadataASTSupplier = Suppliers.memoize(this::loadAST);
    }

    @Override
    public @NonNull IMetadataAST getAST() {
        return metadataASTSupplier.get();
    }

    @NonNull
    private IMetadataAST loadAST() {
        LOGGER.info("Loading metadata from: " + metadataFile.toString());

        final Map<String, MappingToyMetadata.ClassInfo> classInfoMap = GSON.fromJson(
                Exceptions.sneak().get(() -> Files.readString(metadataFile)),
                TYPE_MAP_STRING_CLASSINFO
        );

        return new MappingToyMetadata(classInfoMap);
    }
}
