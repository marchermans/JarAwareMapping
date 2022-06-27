package com.ldtteam.jam.loader;

import com.ldtteam.jam.rename.EnhancedClassRemapper;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.util.FilterUtils;
import com.ldtteam.jam.util.MethodDataUtils;
import com.ldtteam.jam.util.SetsUtil;
import com.machinezoo.noexception.Exceptions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
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
        final Set<ClassData> classes = loadClasses(inputConfiguration);

        //Grab the method datas from them
        final Set<MethodData> methods = classes.stream()
          .flatMap(classData -> classData.node().methods.stream()
                  .map(node -> new MethodData(classData, node)))
          .collect(SetsUtil.methods());

        //And the field datas from them
        final Set<FieldData> fields = classes.stream()
          .flatMap(classData -> classData.node().fields.stream()
                  .map(node -> new FieldData(classData, node)))
          .collect(SetsUtil.fields());

        //Grab the parameter datas from the methods
        final Set<ParameterData> parameters = methods.stream()
                .flatMap(MethodDataUtils::parametersAsStream)
                .collect(SetsUtil.parameters());

        //Collect all of them together.
        return new LoadedASMData(inputConfiguration.name(), classes, methods, fields, parameters);
    }

    /**
     * Loads the classes in the given input configuration from the jar, performing the remapping if
     * the input configuration has a remapper.
     *
     * @param inputConfiguration The input configuration to pull the jar path, and potentially a remapper from.
     * @return The ASM class data data.
     */
    private static Set<ClassData> loadClasses(final InputConfiguration inputConfiguration)
    {
        //Grab and pass along.
        return loadClasses(inputConfiguration.path(), inputConfiguration.remapper().orElse(null));
    }

    private static Set<ClassData> loadClasses(final Path filePath, @Nullable final IRemapper remapperOptional)
    {
        final Path root = getPackageRoot(filePath);
        final Set<ClassData> classesInTarget = new TreeSet<>(Comparator.comparing(classData -> classData.node().name));

        final Function<Path, ClassData> loader = remapperOptional == null ?
                                                   ASMDataLoader::loadClass :
                                                                              path -> loadClass(path, remapperOptional);

        final Optional<IRemapper> remappedHandle = Optional.ofNullable(remapperOptional);

        walk(root)
          .filter(FilterUtils::isClassFile)
          .map(loader)
          .peek(classData -> classData.node().methods.forEach(methodData -> {
              if (methodData.parameters == null)
              {
                  final Type methodDescriptor = Type.getMethodType(methodData.desc);
                  if (methodDescriptor.getArgumentTypes().length > 0)
                  {
                      methodData.parameters = new LinkedList<>();
                      Type[] argumentTypes = methodDescriptor.getArgumentTypes();
                      for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++)
                      {
                          final int index = i;

                          methodData.parameters.add(
                            new ParameterNode(
                              remappedHandle
                                .flatMap(remapper -> remapper.remapParameter(
                                  classData.node().name,
                                  methodData.name,
                                  methodData.desc,
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

    private static ClassData loadClass(final Path classFilePath)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return new ClassData(classNode);
    }

    private static ClassData loadClass(final Path classFilePath, final IRemapper remapper)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        final EnhancedClassRemapper classRemapper = new EnhancedClassRemapper(classNode, remapper);

        classReader.accept(classRemapper, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return new ClassData(classNode);
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
