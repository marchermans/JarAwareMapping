package com.ldtteam.jam.loader;

import com.ldtteam.jam.rename.EnhancedClassRemapper;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.util.FilterUtils;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Loads ASM data from a given input configuration.
 * Primary reason for this system is to collect information on the bytecode contained
 * in the relevant jar, and handle remapping stuff to a common runtime naming scheme.
 */
@SuppressWarnings("resource")
public final class ASMDataLoader
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ASMDataLoader.class);

    private ASMDataLoader()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ASMDataLoader. This is a utility class");
    }

    /**
     * Loads the ASM data from the input configurations jar and naming scheme.
     *
     * @param inputConfiguration The input configuration to load the bytecode data from.
     * @return The loaded ASM data.
     */
    public static LoadedASMData load(final InputConfiguration inputConfiguration)
    {
        //Collect all classes.
        final Set<ClassNode> classes = loadClasses(inputConfiguration);

        //Group the fields and methods into a map, so that we can stable sort them.
        final Map<MethodNode, ClassNode> classNodesByMethodNodes = new HashMap<>();
        final Map<FieldNode, ClassNode> classNodesByFieldNodes = new HashMap<>();
        for (final ClassNode classNode : classes)
        {
            classNode.methods.forEach(node -> classNodesByMethodNodes.put(node, classNode));
            classNode.fields.forEach(node -> classNodesByFieldNodes.put(node, classNode));
        }

        //Grab the method nodes from them
        final Set<MethodNode> methods = new TreeSet<>(Comparator.<MethodNode, String>comparing(node -> classNodesByMethodNodes.get(node).name).thenComparing(node -> node.name + node.desc));
        classes.stream()
          .flatMap(classNode -> classNode.methods.stream())
          .forEach(methods::add);

        //And the field nodes from them
        final Set<FieldNode> fields = new TreeSet<>(Comparator.<FieldNode, String>comparing(node -> classNodesByFieldNodes.get(node).name).thenComparing(node -> node.name + node.desc));
        classes.stream()
          .flatMap(classNode -> classNode.fields.stream())
          .forEach(fields::add);

        //Collect all of them together.
        return new LoadedASMData(inputConfiguration.name(), classes, methods, fields);
    }

    /**
     * Loads the classes in the given input configuration from the jar, performing the remapping if
     * the input configuration has a remapper.
     *
     * @param inputConfiguration The input configuration to pull the jar path, and potentially a remapper from.
     * @return The ASM class node data.
     */
    private static Set<ClassNode> loadClasses(final InputConfiguration inputConfiguration)
    {
        //Grab and pass along.
        return loadClasses(inputConfiguration.path(), inputConfiguration.remapper().orElse(null));
    }

    private static Set<ClassNode> loadClasses(final Path filePath, @Nullable final IRemapper remapperOptional)
    {
        final Path root = getPackageRoot(filePath);
        final Set<ClassNode> classesInTarget = new TreeSet<>(Comparator.comparing(classNode -> classNode.name));

        final Function<Path, ClassNode> loader = remapperOptional == null ?
                                                   ASMDataLoader::loadClass :
                                                                              path -> loadClass(path, remapperOptional);

        final Optional<IRemapper> remappedHandle = Optional.ofNullable(remapperOptional);

        walk(root)
          .filter(FilterUtils::isClassFile)
          .map(loader)
          .peek(classNode -> classNode.methods.forEach(methodNode -> {
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
          }))
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
