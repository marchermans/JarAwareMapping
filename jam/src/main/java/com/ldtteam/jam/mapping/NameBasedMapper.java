package com.ldtteam.jam.mapping;

import com.ldtteam.jam.spi.asm.ClassData;
import com.ldtteam.jam.spi.asm.FieldData;
import com.ldtteam.jam.spi.asm.MethodData;
import com.ldtteam.jam.spi.mapping.IMapper;
import com.ldtteam.jam.spi.name.INameProvider;

import java.util.Optional;
import java.util.Set;

public class NameBasedMapper<T> extends SingleEntryBasedMapper<T>
{

    public static IMapper<ClassData> classes() {
        return new NameBasedMapper<>(INameProvider.classes());
    }

    public static IMapper<MethodData> methods() {
        return new NameBasedMapper<>(INameProvider.methods());
    }

    public static IMapper<FieldData> fields() {
        return new NameBasedMapper<>(INameProvider.fields());
    }

    private final INameProvider<T> nameProvider;

    private NameBasedMapper(final INameProvider<T> nameProvider) {this.nameProvider = nameProvider;}

    @Override
    protected Optional<T> map(final T source, final Set<T> candidates)
    {
        final String sourceName = nameProvider.getName(source);
        return candidates.stream().filter(candidate -> nameProvider.getName(candidate).equals(sourceName)).findFirst();
    }
}
