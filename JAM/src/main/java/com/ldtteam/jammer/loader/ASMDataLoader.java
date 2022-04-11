package com.ldtteam.jammer.loader;

import com.google.common.collect.Sets;
import com.ldtteam.jam.jamspec.configuration.InputConfiguration;
import com.ldtteam.jam.jamspec.name.IRemapper;
import com.ldtteam.jammer.rename.EnhancedClassRemapper;
import com.ldtteam.jammer.util.FilterUtils;
import com.machinezoo.noexception.Exceptions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ASMDataLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ASMDataLoader.class);

    private ASMDataLoader()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ASMDataLoader. This is a utility class");
    }

    public static LoadedASMData load(final InputConfiguration inputConfiguration)
    {
        final Set<ClassNode> classes = loadClasses(inputConfiguration);

        final Set<MethodNode> methods = classes.stream()
          .flatMap(classNode -> classNode.methods.stream())
          .collect(Collectors.toSet());

        final Set<FieldNode> fields = classes.stream()
          .flatMap(classNode -> classNode.fields.stream())
          .collect(Collectors.toSet());

        final Set<ParameterNode> parameters = methods.stream()
          .flatMap(methodNode -> methodNode.parameters == null ? Stream.empty() : methodNode.parameters.stream())
          .collect(Collectors.toSet());

        return new LoadedASMData(inputConfiguration.name(), classes, methods, fields, parameters);
    }

    private static Set<ClassNode> loadClasses(final InputConfiguration inputConfiguration)
    {
        return loadClasses(inputConfiguration.path(), inputConfiguration.remapper().orElse(null));
    }

    private static Set<ClassNode> loadClasses(final Path filePath, @Nullable final IRemapper remapperOptional)
    {
        final Path root = getPackageRoot(filePath);
        final Set<ClassNode> classesInTarget = Sets.newHashSet();

        final Function<Path, ClassNode> loader = remapperOptional == null ?
                                                   ASMDataLoader::loadClass :
                                                                              path -> loadClass(path, remapperOptional);

        final Optional<IRemapper> remappedHandle = Optional.ofNullable(remapperOptional);

        walk(root)
          .filter(FilterUtils::isClassFile)
          .map(loader)
          .peek(classNode -> {
              classNode.methods.forEach(methodNode -> {
                  if (methodNode.parameters == null)
                  {
                      final Type methodDescriptor = Type.getMethodType(methodNode.desc);
                      if (methodDescriptor.getArgumentTypes().length > 0)
                      {
                          methodNode.parameters = new LinkedList<>();
                          Type[] argumentTypes = methodDescriptor.getArgumentTypes();
                          for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++)
                          {
                              final int index = i;

                              methodNode.parameters.add(
                                new ParameterNode(
                                  remappedHandle
                                    .flatMap(remapper -> remapper.remapParameter(
                                      classNode.name,
                                      methodNode.name,
                                      methodNode.desc,
                                      "parameter " + index,
                                      index
                                    ))
                                    .orElse("parameter " + index),
                                  0)
                              );
                          }
                      }
                  }
              });
          })
          .forEach(classesInTarget::add);

        return classesInTarget;
    }

    private static Path getPackageRoot(final Path filePath)
    {
        return Exceptions.log(LOGGER).get(
          Exceptions.sneak().supplier(
            () -> {
                if (filePath.getFileName().toString().endsWith(".jar"))
                {
                    return FileSystems.newFileSystem(filePath, (ClassLoader) null).getPath("/");
                }

                return filePath;
            }
          )
        ).orElse(filePath);
    }

    private static ClassNode loadClass(final Path classFilePath)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return classNode;
    }

    private static ClassNode loadClass(final Path classFilePath, final IRemapper remapper)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        final EnhancedClassRemapper classRemapper = new EnhancedClassRemapper(classNode, remapper);

        classReader.accept(classRemapper, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return classNode;
    }

    private static Stream<Path> walk(final Path path)
    {
        return Exceptions.log(LOGGER).get(
          Exceptions.sneak().supplier(
            () -> Files.walk(path, FileVisitOption.FOLLOW_LINKS)
          )
        ).orElse(Stream.empty());
    }

    private static InputStream createInputStream(final Path classFilePath)
    {
        return Exceptions.sneak().get(() -> Files.newInputStream(classFilePath, StandardOpenOption.READ));
    }

    private static ClassReader createClassReader(final InputStream inputStream)
    {
        return Exceptions.sneak().get(() -> new ClassReader(inputStream));
    }

    private static void closeStream(final InputStream stream)
    {
        Exceptions.sneak().run(stream::close);
    }
}
