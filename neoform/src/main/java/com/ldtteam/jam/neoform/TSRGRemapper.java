package com.ldtteam.jam.neoform;

import com.ldtteam.jam.spi.ast.metadata.IMetadataAST;
import com.ldtteam.jam.spi.ast.metadata.IMetadataClass;
import com.ldtteam.jam.spi.name.IRemapper;
import com.machinezoo.noexception.Exceptions;
import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.INamedMappingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public class TSRGRemapper implements IRemapper {

    private final IMappingFile remapper;
    private final IMetadataAST metadata;

    private TSRGRemapper(final Path mappingFilePath, final String sourceSide, final String targetSide, IMetadataAST metadata) {
        this.metadata = metadata;
        INamedMappingFile mappingFile = Exceptions.sneak().get(() -> INamedMappingFile.load(Files.newInputStream(mappingFilePath)));
        remapper = mappingFile.getMap(sourceSide, targetSide);
    }

    private TSRGRemapper(final IMappingFile remapper, IMetadataAST metadata) {
        this.remapper = remapper;
        this.metadata = metadata;
    }

    public static IRemapper createObfuscatedToOfficial(Path path, IMetadataAST metadata) {
        return new TSRGRemapper(path, "right", "left", metadata);
    }

    public static IRemapper createOfficialToObfuscated(Path path, IMetadataAST metadata) {
        return new TSRGRemapper(path, "left", "right", metadata);
    }

    @Override
    public Optional<String> remapClass(final String className) {
        return Optional.ofNullable(remapper.remapClass(className));
    }

    @Override
    public Optional<String> remapMethod(final String className, final String name, final String descriptor) {
        return remapUsingSuper(className, name, c -> c.remapMethod(name, descriptor), superClassName -> remapMethod(superClassName, name, descriptor), true);
    }

    @Override
    public Optional<String> remapDescriptor(String descriptor) {
        return Optional.ofNullable(remapper.remapDescriptor(descriptor));
    }

    @Override
    public Optional<String> remapField(final String className, final String name, final String type) {
        return remapUsingSuper(className, name, c -> c.remapField(name), superClassName -> remapField(superClassName, name, type), false);
    }
    
    private Optional<String> remapUsingSuper(String className, String name, Function<IMappingFile.IClass, String> remap, Function<String, Optional<String>> recurse, boolean interfaces) {
        return Optional.ofNullable(remapper.getClass(className))
                .map(remap::apply)
                .map(f -> {
                    //Check if we remapped
                    if (!f.equals(name))
                        return f;

                    //No remapping happened, maybe we do not have this field.
                    //Let's check the super class.
                    final IMetadataClass classMetadata = metadata.getClassesByName().get(className);

                    //No data available, we can't do anything.
                    if (classMetadata == null)
                        return name;

                    //Check if we have a super class.
                    final String superClassName = classMetadata.getSuperName();
                    if (superClassName == null)
                        return name;

                    //Check if the super class has the field.
                    Optional<String> superClass = recurse.apply(superClassName);
                    if (superClass.isPresent()) {
                        return superClass.get();
                    }

                    //Check interfaces
                    if (interfaces && classMetadata.getInterfaces() != null) {
                        for (String interfaceClass : classMetadata.getInterfaces()) {
                            Optional<String> interfaceClassMethod = recurse.apply(interfaceClass);
                            if (interfaceClassMethod.isPresent()) {
                                return interfaceClassMethod.get();
                            }
                        }
                    }
                    return name;
                });
    }

    @Override
    public Optional<String> remapParameter(final String className, final String methodName, final String descriptor, final String parameterName, final int index) {
        return Optional.of("o");
    }

    @Override
    public Optional<String> remapPackage(final String packageName) {
        return Optional.ofNullable(remapper.remapPackage(packageName));
    }

    @Override
    public IRemapper reverse() {
        return new TSRGRemapper(remapper.reverse(), metadata);
    }
}
