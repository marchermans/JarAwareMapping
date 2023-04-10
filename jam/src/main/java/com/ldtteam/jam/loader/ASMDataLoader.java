package com.ldtteam.jam.loader;

import com.ldtteam.jam.rename.EnhancedClassRemapper;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.asm.ParameterData;
import com.ldtteam.jam.spi.configuration.InputConfiguration;
import com.ldtteam.jam.spi.name.IRemapper;
import com.ldtteam.jam.spi.payload.IPayloadSupplier;
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
    public static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> LoadedASMData<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> load(final InputConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> inputConfiguration)
    {
        final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloadSupplier = inputConfiguration.payloads().orElse(IPayloadSupplier.empty());
        //Collect all classes.
        final Set<ClassData<TClassPayload>> classes = loadClasses(inputConfiguration);

        //Grab the method datas from them
        final Set<MethodData<TClassPayload, TMethodPayload>> methods = classes.stream()
          .flatMap(classData -> classData.node().methods.stream()
                  .map(node -> new MethodData<>(classData, node, payloadSupplier.forMethod(classData.node(), node))))
          .collect(SetsUtil.methods());

        //And the field datas from them
        final Set<FieldData<TClassPayload, TFieldPayload>> fields = classes.stream()
          .flatMap(classData -> classData.node().fields.stream()
                  .map(node -> new FieldData<>(classData, node, payloadSupplier.forField(classData.node(), node))))
          .collect(SetsUtil.fields());

        //Grab the parameter datas from the methods
        final Set<ParameterData<TClassPayload, TMethodPayload, TParameterPayload>> parameters = methods.stream()
                .flatMap(methodData -> MethodDataUtils.parametersAsStream(methodData, payloadSupplier))
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
    private static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> Set<ClassData<TClassPayload>> loadClasses(final InputConfiguration<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> inputConfiguration)
    {
        //Grab and pass along.
        return loadClasses(inputConfiguration.path(), inputConfiguration.remapper().orElse(null), inputConfiguration.payloads().orElse(IPayloadSupplier.empty()));
    }

    /**
     * Loads the classes in the given jar, performing the remapping if
     * the input configuration has a remapper.
     *
     * @param filePath The path to the jar to load the classes from.
     * @param remapperOptional The optional remapper to use.
     * @return The ASM class data data.
     */
    private static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> Set<ClassData<TClassPayload>> loadClasses(final Path filePath, @Nullable final IRemapper remapperOptional, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads)
    {
        final Path root = getPackageRoot(filePath);
        final Set<ClassData<TClassPayload>> classesInTarget = new TreeSet<>(Comparator.comparing(classData -> classData.node().name));

        final Function<Path, ClassData<TClassPayload>> loader = remapperOptional == null ?
                                                   path -> loadClass(path, payloads) :
                                                                              path -> loadClass(path, remapperOptional, payloads);

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

    /**
     * Determines the root package in a given file path.
     *
     * @param filePath The file path to determine the root package of.
     * @return The root package.
     */
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

    /**
     * Loads the class data from the given class file path.
     *
     * @param classFilePath The path to the class file to load.
     * @return The ASM class data.
     */
    private static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> ClassData<TClassPayload> loadClass(final Path classFilePath, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        classReader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return new ClassData<>(classNode, payloads.forClass(classNode));
    }

    /**
     * Loads the class data from the given class file path.
     *
     * @param classFilePath The path to the class file to load.
     * @param remapper The remapper to use.
     * @return The ASM class data.
     */
    private static <TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> ClassData<TClassPayload> loadClass(final Path classFilePath, final IRemapper remapper, final IPayloadSupplier<TClassPayload, TFieldPayload, TMethodPayload, TParameterPayload> payloads)
    {
        final InputStream stream = createInputStream(classFilePath);
        final ClassReader classReader = createClassReader(stream);
        final ClassNode classNode = new ClassNode();

        final EnhancedClassRemapper classRemapper = new EnhancedClassRemapper(classNode, remapper);

        classReader.accept(classRemapper, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);

        closeStream(stream);

        return new ClassData<>(classNode, payloads.forClass(classNode));
    }

    /**
     * Walks the given path, returning a stream of all the paths.
     *
     * @param path The path to walk.
     * @return The stream of paths.
     */
    private static Stream<Path> walk(final Path path)
    {
        return Exceptions.log(LOGGER).get(
          Exceptions.sneak().supplier(
            () -> Files.walk(path, FileVisitOption.FOLLOW_LINKS)
          )
        ).orElse(Stream.empty());
    }

    /**
     * Creates an input stream from the given path.
     *
     * @param classFilePath The path to create the input stream from.
     * @return The input stream.
     */
    private static InputStream createInputStream(final Path classFilePath)
    {
        return Exceptions.sneak().get(() -> Files.newInputStream(classFilePath, StandardOpenOption.READ));
    }

    /**
     * Creates a class reader from the given input stream.
     *
     * @param inputStream The input stream to create the class reader from.
     * @return The class reader.
     */
    private static ClassReader createClassReader(final InputStream inputStream)
    {
        return Exceptions.sneak().get(() -> new ClassReader(inputStream));
    }

    /**
     * Closes the given input stream.
     *
     * @param stream The input stream to close.
     */
    private static void closeStream(final InputStream stream)
    {
        Exceptions.sneak().run(stream::close);
    }
}
