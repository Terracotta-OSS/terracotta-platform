/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.json.gson.internal;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ReflectionAccessFilter.FilterResult;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.internal.ReflectionAccessFilterHelper;
import com.google.gson.internal.bind.TreeTypeAdapter;
import com.google.gson.internal.reflect.ReflectionHelper;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.terracotta.json.gson.Adapters;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/**
 * This Gson adapter is highly inspired from
 * {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory}
 * and adapted to add Mixin support
 * <p>
 * A mixin can override some fields from its superclass to define another
 * adapter with {@link com.google.gson.annotations.JsonAdapter}
 */
public final class MixinTypeAdapterFactory implements TypeAdapterFactory {

  public static final Excluder MIXIN_EXCLUDER = new Excluder()
      .withExclusionStrategy(ExclusionStrategies.EXPOSE_ANNOTATION_SERIALIZE, true, false)
      .withExclusionStrategy(ExclusionStrategies.EXPOSE_ANNOTATION_DESERIALIZE, false, true);

  private final Class<?> type;
  private final Class<?> mixin;
  private final boolean sortKeys;
  private final ConstructorConstructor constructorConstructor;
  private final FieldNamingStrategy fieldNamingPolicy;
  private final Excluder excluder;
  private final List<ReflectionAccessFilter> reflectionFilters = Adapters.DEFAULT_ACCESS_FILTERS;
  private final Map<String, Field> mixinFields = new HashMap<>();

  public MixinTypeAdapterFactory(Class<?> type, Class<?> mixin) {
    this(type, mixin, true, emptyMap(), FieldNamingPolicy.IDENTITY, MIXIN_EXCLUDER);
  }

  private MixinTypeAdapterFactory(Class<?> type, Class<?> mixin,
      boolean sortKeys,
      Map<Type, InstanceCreator<?>> instanceCreators,
      FieldNamingStrategy fieldNamingPolicy,
      Excluder excluder) {
    requireNonNull(type);
    requireNonNull(mixin);
    requireNonNull(instanceCreators);
    requireNonNull(fieldNamingPolicy);
    requireNonNull(excluder);
    this.type = type;
    this.mixin = mixin;
    this.sortKeys = sortKeys;
    this.constructorConstructor = new ConstructorConstructor(instanceCreators, false, Adapters.DEFAULT_ACCESS_FILTERS);
    this.fieldNamingPolicy = fieldNamingPolicy;
    this.excluder = excluder;

    for (Field f : mixin.getDeclaredFields()) {
      mixinFields.put(f.getName(), f);
    }
  }

  @Override
  public String toString() {
    return "Factory[type=" + type + ",mixin=" + mixin + "]";
  }

  private Optional<Field> getMixinField(Field typeField) {
    return Optional.ofNullable(mixinFields.get(typeField.getName()));
  }

  private boolean includeField(Field f, boolean serialize) {
    return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
  }

  /**
   * first element holds the default name
   */
  private List<String> getFieldNames(Field f) {
    SerializedName annotation = f.getAnnotation(SerializedName.class);
    if (annotation == null) {
      String name = fieldNamingPolicy.translateName(f);
      return Collections.singletonList(name);
    }

    String serializedName = annotation.value();
    String[] alternates = annotation.alternate();
    if (alternates.length == 0) {
      return Collections.singletonList(serializedName);
    }

    List<String> fieldNames = new ArrayList<>(alternates.length + 1);
    fieldNames.add(serializedName);
    Collections.addAll(fieldNames, alternates);
    return fieldNames;
  }

  @Override
  public <R> TypeAdapter<R> create(Gson gson, final TypeToken<R> type) {
    Class<? super R> raw = type.getRawType();

    if (this.type != raw) {
      return null;
    }

    FilterResult filterResult = ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
    if (filterResult == FilterResult.BLOCK_ALL) {
      throw new JsonIOException(
          "ReflectionAccessFilter does not permit using reflection for " + raw
              + ". Register a TypeAdapter for this type or adjust the access filter.");
    }
    boolean blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;

    ObjectConstructor<R> constructor = constructorConstructor.get(type);
    return new FieldReflectionTypeAdapter<>(constructor, getBoundFields(gson, type, raw, blockInaccessible));
  }

  private static <M extends AccessibleObject & Member> void checkAccessible(Object object, M member) {
    if (!ReflectionAccessFilterHelper.canAccess(member, Modifier.isStatic(member.getModifiers()) ? null : object)) {
      String memberDescription = ReflectionHelper.getAccessibleObjectDescription(member, true);
      throw new JsonIOException(memberDescription + " is not accessible and ReflectionAccessFilter does not"
          + " permit making it accessible. Register a TypeAdapter for the declaring type, adjust the"
          + " access filter or increase the visibility of the element and its declaring type.");
    }
  }

  private <T> BoundField createBoundField(
      final Gson context, final Field field, final String name,
      final TypeToken<T> fieldType, boolean serialize, boolean deserialize,
      final boolean blockInaccessible) {
    final Field mixinField = getMixinField(field).orElse(field);

    final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());

    int modifiers = field.getModifiers();
    final boolean isStaticFinalField = Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers);

    JsonAdapter annotation = mixinField.getAnnotation(JsonAdapter.class);
    if (annotation == null) {
      annotation = field.getAnnotation(JsonAdapter.class);
    }
    TypeAdapter<T> mapped = null;
    if (annotation != null) {
      // This is not safe; requires that user has specified correct adapter class for
      // @JsonAdapter
      mapped = getTypeAdapter(constructorConstructor, context, fieldType, annotation);
    }
    final boolean jsonAdapterPresent = mapped != null;
    if (mapped == null)
      mapped = context.getAdapter(fieldType);

    final TypeAdapter<T> typeAdapter = mapped;
    return new BoundField(name, field, serialize, deserialize) {
      @SuppressWarnings("unchecked")
      @Override
      void write(JsonWriter writer, Object source)
          throws IOException, IllegalAccessException {
        if (!serialized)
          return;
        if (blockInaccessible) {
          checkAccessible(source, field);
        }

        T fieldValue = (T) field.get(source);
        if (fieldValue == source) {
          // avoid direct recursion
          return;
        }
        writer.name(name);
        TypeAdapter<T> t = jsonAdapterPresent ? typeAdapter : new RuntimeTypeAdapter<>(context, typeAdapter, fieldType);
        t.write(writer, fieldValue);
      }

      @Override
      void readIntoField(JsonReader reader, Object target)
          throws IOException, IllegalAccessException {
        Object fieldValue = typeAdapter.read(reader);
        if (fieldValue != null || !isPrimitive) {
          if (blockInaccessible) {
            checkAccessible(target, field);
          } else if (isStaticFinalField) {
            // Reflection does not permit setting value of `static final` field, even after
            // calling `setAccessible`
            // Handle this here to avoid causing IllegalAccessException when calling
            // `Field.set`
            String fieldDescription = ReflectionHelper.getAccessibleObjectDescription(field, false);
            throw new JsonIOException("Cannot set value of 'static final' " + fieldDescription);
          }
          field.set(target, fieldValue);
        }
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T> TypeAdapter<T> getTypeAdapter(ConstructorConstructor constructorConstructor, Gson gson, TypeToken<T> type,
      JsonAdapter annotation) {
    Object instance = constructorConstructor.get(TypeToken.get(annotation.value())).construct();

    TypeAdapter<T> typeAdapter;
    boolean nullSafe = annotation.nullSafe();
    if (instance instanceof TypeAdapter) {
      typeAdapter = (TypeAdapter<T>) instance;
    } else if (instance instanceof TypeAdapterFactory) {
      typeAdapter = ((TypeAdapterFactory) instance).create(gson, type);
    } else if (instance instanceof JsonSerializer || instance instanceof JsonDeserializer) {
      JsonSerializer<?> serializer = instance instanceof JsonSerializer
          ? (JsonSerializer<?>) instance
          : null;
      JsonDeserializer<?> deserializer = instance instanceof JsonDeserializer
          ? (JsonDeserializer<?>) instance
          : null;

      @SuppressWarnings({ "unchecked", "rawtypes" })
      TypeAdapter<T> tempAdapter = new TreeTypeAdapter(serializer, deserializer, gson, type, null, nullSafe);
      typeAdapter = tempAdapter;

      nullSafe = false;
    } else {
      throw new IllegalArgumentException("Invalid attempt to bind an instance of "
          + instance.getClass().getName() + " as a @JsonAdapter for " + type.toString()
          + ". @JsonAdapter value must be a TypeAdapter, TypeAdapterFactory,"
          + " JsonSerializer or JsonDeserializer.");
    }

    if (typeAdapter != null && nullSafe) {
      typeAdapter = Adapters.nullSafe(typeAdapter);
    }

    return typeAdapter;
  }

  private Map<String, BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw,
      boolean blockInaccessible) {
    Map<String, BoundField> result = sortKeys ? new TreeMap<>() : new LinkedHashMap<>();
    if (raw.isInterface()) {
      return result;
    }

    Class<?> originalRaw = raw;
    while (raw != Object.class) {
      Field[] fields = raw.getDeclaredFields();

      // For inherited fields, check if access to their declaring class is allowed
      if (raw != originalRaw && fields.length > 0) {
        FilterResult filterResult = ReflectionAccessFilterHelper.getFilterResult(reflectionFilters, raw);
        if (filterResult == FilterResult.BLOCK_ALL) {
          throw new JsonIOException("ReflectionAccessFilter does not permit using reflection for " + raw
              + " (supertype of " + originalRaw + "). Register a TypeAdapter for this type"
              + " or adjust the access filter.");
        }
        blockInaccessible = filterResult == FilterResult.BLOCK_INACCESSIBLE;
      }

      for (Field field : fields) {
        final Field mixinField = getMixinField(field).orElse(field);
        boolean serialize = includeField(field, true) && includeField(mixinField, true);
        boolean deserialize = includeField(field, false) && includeField(mixinField, false);
        if (!serialize && !deserialize) {
          continue;
        }
        // If blockInaccessible, skip and perform access check later
        if (!blockInaccessible) {
          ReflectionHelper.makeAccessible(field);
          ReflectionHelper.makeAccessible(mixinField);
        }
        Type fieldType = $Gson$Types.resolve(type.getType(), raw, mixinField.getGenericType());
        List<String> fieldNames = getFieldNames(mixinField);
        BoundField previous = null;
        for (int i = 0, size = fieldNames.size(); i < size; ++i) {
          String name = fieldNames.get(i);
          if (i != 0)
            serialize = false; // only serialize the default name
          BoundField boundField = createBoundField(context, field, name,
              TypeToken.get(fieldType), serialize, deserialize, blockInaccessible);
          BoundField replaced = result.put(name, boundField);
          if (previous == null)
            previous = replaced;
        }
        if (previous != null) {
          throw new IllegalArgumentException("Class " + originalRaw.getName()
              + " declares multiple JSON fields named '" + previous.name + "'; conflict is caused"
              + " by fields " + ReflectionHelper.fieldToString(previous.field) + " and "
              + ReflectionHelper.fieldToString(field));
        }
      }
      type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
      raw = type.getRawType();
    }
    return result;
  }

  static abstract class BoundField {
    final String name;
    final Field field;
    /**
     * Name of the underlying field
     */
    final String fieldName;
    final boolean serialized;
    final boolean deserialized;

    protected BoundField(String name, Field field, boolean serialized, boolean deserialized) {
      this.name = name;
      this.field = field;
      this.fieldName = field.getName();
      this.serialized = serialized;
      this.deserialized = deserialized;
    }

    /**
     * Read this field value from the source, and append its JSON value to the
     * writer
     */
    abstract void write(JsonWriter writer, Object source) throws IOException, IllegalAccessException;

    /**
     * Read the value from the reader, and set it on the corresponding field on
     * target via reflection
     */
    abstract void readIntoField(JsonReader reader, Object target) throws IOException, IllegalAccessException;
  }

  /**
   * Copy of
   * {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.FieldAdapter}
   * for the {@link @org.terracotta.json.gson.internal.MixinTypeAdapterFactory}
   * <p>
   * Base class for Adapters produced by this factory.
   *
   * @param <T> type of objects that this Adapter creates.
   * @param <A> type of accumulator used to build the deserialization result.
   */
  private abstract static class FieldTypeAdapter<T, A> extends TypeAdapter<T> {
    final Map<String, BoundField> boundFields;

    FieldTypeAdapter(Map<String, BoundField> boundFields) {
      this.boundFields = boundFields;
    }

    @Override
    public String toString() {
      return getClass().getName() + ":" + boundFields.keySet();
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }

      out.beginObject();
      try {
        for (BoundField boundField : boundFields.values()) {
          boundField.write(out, value);
        }
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }

      A accumulator = createAccumulator();

      try {
        in.beginObject();
        while (in.hasNext()) {
          String name = in.nextName();
          BoundField field = boundFields.get(name);
          if (field == null || !field.deserialized) {
            in.skipValue();
          } else {
            readField(accumulator, in, field);
          }
        }
      } catch (IllegalStateException e) {
        throw new JsonSyntaxException(e);
      } catch (IllegalAccessException e) {
        throw ReflectionHelper.createExceptionForUnexpectedIllegalAccess(e);
      }
      in.endObject();
      return finalize(accumulator);
    }

    /**
     * Create the Object that will be used to collect each field value
     */
    abstract A createAccumulator();

    /**
     * Read a single BoundField into the accumulator. The JsonReader will be pointed
     * at the
     * start of the value for the BoundField to read from.
     */
    abstract void readField(A accumulator, JsonReader in, BoundField field) throws IllegalAccessException, IOException;

    /**
     * Convert the accumulator to a final instance of T.
     */
    abstract T finalize(A accumulator);
  }

  /**
   * Copy of
   * {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.FieldReflectionAdapter}
   * to use for the
   * {@link @org.terracotta.json.gson.internal.MixinTypeAdapterFactory}
   */
  private static final class FieldReflectionTypeAdapter<T> extends FieldTypeAdapter<T, T> {
    private final ObjectConstructor<T> constructor;

    FieldReflectionTypeAdapter(ObjectConstructor<T> constructor, Map<String, BoundField> boundFields) {
      super(boundFields);
      this.constructor = constructor;
    }

    @Override
    T createAccumulator() {
      return constructor.construct();
    }

    @Override
    void readField(T accumulator, JsonReader in, BoundField field) throws IllegalAccessException, IOException {
      field.readIntoField(in, accumulator);
    }

    @Override
    T finalize(T accumulator) {
      return accumulator;
    }
  }

}
