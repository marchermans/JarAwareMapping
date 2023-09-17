package com.ldtteam.jam.neoform;

import com.ldtteam.jam.ast.NamedClassBuilder;
import com.ldtteam.jam.ast.NamedFieldBuilder;
import com.ldtteam.jam.ast.NamedMethodBuilder;
import com.ldtteam.jam.ast.NamedParameterBuilder;
import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.name.INameProvider;
import com.ldtteam.jam.spi.name.IRemapper;

import java.util.function.Function;

public class TSRGObfuscatedNameProvider<T> implements INameProvider<T> {
   
   public static INameProvider<NamedClassBuilder.ClassNamingInformation> classes(IRemapper runtimeToObfuscatedRemapper) {
      return new TSRGObfuscatedNameProvider<>(
            (data) -> runtimeToObfuscatedRemapper.remapClass(data.target().node().name).orElseThrow()
      );
   }
   
   public static INameProvider<NamedFieldBuilder.FieldNamingInformation> fields(IRemapper runtimeToObfuscatedRemapper) {
      return new TSRGObfuscatedNameProvider<>(
            (data) -> {
               return runtimeToObfuscatedRemapper.remapField(
                  data.target().owner().node().name,
                     data.target().node().name,
                     data.target().node().desc
               ).orElseThrow();
            }
      );
   }
   
   public static INameProvider<NamedMethodBuilder.MethodNamingInformation> methods(IRemapper runtimeToObfuscatedRemapper) {
      return new TSRGObfuscatedNameProvider<>(
            (data) -> {
               return runtimeToObfuscatedRemapper.remapMethod(
                     data.target().owner().node().name,
                     data.target().node().name,
                     data.target().node().desc
               ).orElseThrow();
            }
      );
   }
   
   public static INameProvider<NamedParameterBuilder.ParameterNamingInformation> parameters() {
      return (data) -> "a";
   }
   
   private final Function<T, String> defaultNameFormatter;
   
   private TSRGObfuscatedNameProvider(final Function<T, String> defaultNameFormatter) {
      this.defaultNameFormatter = defaultNameFormatter;
   }
   
   
   @Override
   public String getName(final T t) {
      return defaultNameFormatter.apply(t);
   }
}
