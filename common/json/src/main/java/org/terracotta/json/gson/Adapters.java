/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.json.gson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.ReflectionAccessFilter;
import com.google.gson.ToNumberStrategy;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ReflectionAccessFilterHelper;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.terracotta.json.Json;
import org.terracotta.json.gson.internal.AllowedClassTypeAdapter;
import org.terracotta.json.gson.internal.FloatingPointTypeAdapter;
import org.terracotta.json.gson.internal.HierarchyTypeAdapterFactory;
import org.terracotta.json.gson.internal.MixinTypeAdapterFactory;
import org.terracotta.json.gson.internal.NullSafeTypeAdapter;
import org.terracotta.json.gson.internal.NullTypeAdapter;
import org.terracotta.json.gson.internal.ObjectToLongTypeAdapter;
import org.terracotta.json.gson.internal.ObjectToStringTypeAdapter;
import org.terracotta.json.gson.internal.OptionalTypeAdapter;
import org.terracotta.json.gson.internal.PathTypeAdapter;
import org.terracotta.json.gson.internal.SortKeysTypeAdapter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.google.gson.internal.bind.TypeAdapters.newTypeHierarchyFactory;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class Adapters {
  private static final TypeToken<?> WILD_TYPE_TOKEN = TypeToken.get(Object.class);
  private static final Collection<String> SPECIAL_NUMBERS = new HashSet<>(asList("NaN", "Infinity", "-Infinity"));

  public static final List<ReflectionAccessFilter> DEFAULT_ACCESS_FILTERS = Collections.unmodifiableList(Arrays.asList(
      ReflectionAccessFilter.BLOCK_ALL_JAVA,
      ReflectionAccessFilter.BLOCK_INACCESSIBLE_JAVA,
      ReflectionAccessFilter.BLOCK_ALL_ANDROID,
      ReflectionAccessFilter.BLOCK_ALL_PLATFORM
  ));

  public static final ToNumberStrategy JACKSON_LIKE_NUMBER_STRATEGY = in -> {
    String value = in.nextString();
    if (value.contains(".") || SPECIAL_NUMBERS.contains(value)) {
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException e) {
        throw new JsonParseException("Cannot parse decimal number " + value + "; at path " + in.getPreviousPath(), e);
      }
    } else {
      try {
        BigInteger exact = new BigInteger(value, 10);
        try {
          return exact.intValueExact();
        } catch (ArithmeticException e) {
          try {
            return exact.longValueExact();
          } catch (ArithmeticException f) {
            return exact;
          }
        }
      } catch (NumberFormatException e) {
        throw new JsonParseException("Cannot parse number " + value + "; at path " + in.getPreviousPath(), e);
      }
    }
  };

  /**
   * Factory which must be registered first and will wrap all subsequent ones to order json object keys
   */
  public static final TypeAdapterFactory SORT_KEYS = new TypeAdapterFactory() {
    @Override
    public String toString() {
      return "Factory[type=ALL,adapter=" + SortKeysTypeAdapter.class.getName() + "]";
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      final Class<? super T> rawType = type.getRawType();
      if (rawType.isPrimitive()
          || rawType.isArray()
          || Number.class.isAssignableFrom(rawType)
          || String.class == type.getRawType()
          || Boolean.class == type.getRawType()
          || Collection.class.isAssignableFrom(rawType)
          || rawType == Json.Null.class) {
        return null;
      }
      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
      final TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
      return nullSafe(new SortKeysTypeAdapter<>(delegate, jsonElementTypeAdapter, gson.serializeNulls()));
    }
  };

  /**
   * Path support
   */
  public static final TypeAdapterFactory PATH = newTypeHierarchyFactory(Path.class, nullSafe(new PathTypeAdapter()));

  /**
   * Optional support
   */
  public static final TypeAdapterFactory OPTIONAL = new TypeAdapterFactory() {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      final Class<? super T> rawType = type.getRawType();
      if (rawType != Optional.class) {
        return null;
      }
      final Type genericType = type.getType();
      final TypeToken<?> token = genericType instanceof ParameterizedType ? TypeToken.get(((ParameterizedType) type.getType()).getActualTypeArguments()[0]) : null;
      return (TypeAdapter<T>) createAdapter(gson, token);
    }

    private <V> TypeAdapter<Optional<V>> createAdapter(Gson gson, TypeToken<V> token) {
      final TypeAdapter<V> delegate = token == null ? null : gson.getAdapter(token);
      return nullSafe(new OptionalTypeAdapter<>(gson, token, delegate));
    }

    @Override
    public String toString() {
      return "Factory[type=" + Optional.class.getName() + ",adapter=" + OptionalTypeAdapter.class.getName() + "]";
    }
  };

  public static final TypeAdapter<Double> DOUBLES = nullSafe(new FloatingPointTypeAdapter<Double>() {
    @Override
    protected Double convert(Number n) {
      return n.doubleValue();
    }
  });

  public static final TypeAdapter<Float> FLOATS = nullSafe(new FloatingPointTypeAdapter<Float>() {
    @Override
    protected Float convert(Number n) {
      return n.floatValue();
    }
  });

  public static final TypeAdapter<Double> PLAIN_DOUBLES = nullSafe(new FloatingPointTypeAdapter<Double>(true) {
    @Override
    protected Double convert(Number n) {
      return n.doubleValue();
    }
  });

  public static final TypeAdapter<Float> PLAIN_FLOATS = nullSafe(new FloatingPointTypeAdapter<Float>(true) {
    @Override
    protected Float convert(Number n) {
      return n.floatValue();
    }
  });

  public static ReflectionAccessFilter.FilterResult getFilterResult(Class<?> rawType) {
    return ReflectionAccessFilterHelper.getFilterResult(DEFAULT_ACCESS_FILTERS, rawType);
  }

  public static ReflectionAccessFilter.FilterResult getFilterResult(TypeToken<?> token) {
    return getFilterResult(token.getRawType());
  }

  /**
   * Force Gson to write "null" when this type is found (usually in a list or map), even if Gson is configured to not write nulls.
   */
  public static <T> TypeAdapterFactory writeNull(Class<T> type) {
    requireNonNull(type);
    return TypeAdapters.newTypeHierarchyFactory(type, new NullTypeAdapter<>());
  }

  public static <T> TypeAdapterFactory objectToString(TypeToken<T> type, Function<String, T> reverse) {
    return objectToString(type, Objects::toString, reverse);
  }

  /**
   * Add a serializer for a type using toString(), and a deserializer using the reverse mapping function supplied
   */
  public static <T> TypeAdapterFactory objectToString(TypeToken<T> type, Function<T, String> stringifier, Function<String, T> reverse) {
    requireNonNull(type);
    requireNonNull(stringifier);
    requireNonNull(reverse);
    return newFactory(type, new ObjectToStringTypeAdapter<>(stringifier, reverse));
  }

  /**
   * Add a serializer for a type using a long, and a deserializer using the reverse mapping function supplied
   */
  public static <T> TypeAdapterFactory objectToLong(TypeToken<T> type, Function<T, Long> longifier, Function<Long, T> reverse) {
    requireNonNull(type);
    requireNonNull(longifier);
    requireNonNull(reverse);
    return newFactory(type, new ObjectToLongTypeAdapter<>(longifier, reverse));
  }

  public static <TT> TypeAdapterFactory newFactory(final TypeToken<TT> type, final TypeAdapter<TT> typeAdapter) {
    final TypeAdapter<TT> adapter = nullSafe(typeAdapter);
    return new TypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        return typeToken.equals(type) ? (TypeAdapter<T>) adapter : null;
      }

      @Override
      public String toString() {
        return "Factory[type=" + type + ",adapter=" + adapter + "]";
      }
    };
  }

  public static <T> TypeAdapter<T> nullSafe(final TypeAdapter<T> adapter) {
    return new NullSafeTypeAdapter<>(adapter);
  }

  /**
   * Allow this type to be deserialized by using a @class attribute and class loading.
   * Use in conjunction with {@link #allowClassLoading(ClassLoader, Collection)}
   */
  public static <T> TypeAdapterFactory registerUnsafeTypeAdapter(TypeToken<T> type) {
    return new UnsafeObjectTypeAdapterFactory<>(type);
  }

  /**
   * Allow these classes to be serialized and deserialized with to their name and loaded back with a given classloader.
   * <p>
   * Gson does not allow by default any class deserialization, and we must specifically allow them.
   */
  public static TypeAdapterFactory allowClassLoading(ClassLoader classLoader, Collection<String> allowed) {
    requireNonNull(classLoader);
    requireNonNull(allowed);
    if (allowed.isEmpty()) {
      throw new IllegalArgumentException("No classes allowed");
    }
    final Collection<String> supported = new HashSet<>(allowed);
    return new TypeAdapterFactory() {
      @Override
      public String toString() {
        return "Factory[type=" + Class.class.getName() + ",adapter=" + AllowedClassTypeAdapter.class.getName() + ",allowed=" + supported + "]";
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == Class.class) {
          final TypeAdapter<Class<?>> delegate = gson.getDelegateAdapter(this, new TypeToken<Class<?>>() {});
          return (TypeAdapter<T>) new AllowedClassTypeAdapter(gson, delegate, classLoader, supported);
        }
        return null;
      }
    };
  }

  /**
   * Support for serializing and deserializing a type hierarchy
   */
  public static <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> superType, String typeFieldName, Function<Class<? extends T>, String> mapper) {
    return HierarchyTypeAdapterFactory.of(superType, typeFieldName, true, true, mapper).recognizeSubtypes();
  }

  public static <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> superType, String typeFieldName) {
    return registerSuperType(superType, typeFieldName, Class::getSimpleName);
  }

  public static <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> superType) {
    return registerSuperType(superType, "type", Class::getSimpleName);
  }

  /**
   * Support for serializing a type hierarchy but prevents any deserialization
   */
  public static <T> TypeAdapterFactory serializeSubtypes(TypeToken<T> superType) {
    requireNonNull(superType);
    return mapSuperType(superType, new Function<JsonObject, TypeToken<? extends T>>() {
      @Override
      public String toString() {
        return superType + "<->" + superType;
      }

      @Override
      public TypeToken<? extends T> apply(JsonObject jsonObject) {
        return superType;
      }
    });
  }

  /**
   * Deserialize a type by using a specific deserializer
   */
  public static <T> TypeAdapterFactory mapSuperType(TypeToken<T> baseType, JsonObjectDeserializer<T> deserializer) {
    requireNonNull(baseType);
    requireNonNull(deserializer);
    return new DelegateTypeAdapterFactory<T>(baseType, true) {
      @Override
      public String toString() {
        return "Factory[type=" + type + ",adapter=mapSuperType(" + deserializer + ")]";
      }

      @Override
      public TypeAdapter<T> create(Gson gson) {
        final TypeAdapterFactory that = this;
        final TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
        return new TypeAdapter<T>() {
          @SuppressWarnings("unchecked")
          @Override
          public void write(JsonWriter out, T value) throws IOException {
            final boolean subType = baseType.getRawType() != value.getClass();
            final TypeToken<T> runtimeType = TypeToken.get((Class<T>) value.getClass());
            final TypeAdapter<T> runtimeDelegate = subType ?
                gson.getDelegateAdapter(that, runtimeType) :
                gson.getDelegateAdapter(that, type);
            runtimeDelegate.write(out, value);
          }

          @Override
          public T read(JsonReader in) throws IOException {
            final JsonObject object = jsonElementTypeAdapter.read(in).getAsJsonObject();
            return deserializer.deserialize(object, gson, that);
          }

          @Override
          public String toString() {
            return deserializer.toString();
          }
        };
      }
    };
  }

  /**
   * Deserialize a type by using a specific deserializer of one subtype.
   * Useful when there is only one implementation of a super type.
   */
  public static <T> TypeAdapterFactory mapSuperType(TypeToken<T> superType, TypeToken<? extends T> subType) {
    return mapSuperType(superType, new Function<JsonObject, TypeToken<? extends T>>() {
      @Override
      public TypeToken<? extends T> apply(JsonObject jsonObject) {
        return subType;
      }

      @Override
      public String toString() {
        return superType + "<->" + subType;
      }
    });
  }

  /**
   * Deserialize a type by using a specific deserializer of a selected subtype.
   * Useful when there is only a choice of implementation of a super type.
   */
  public static <T> TypeAdapterFactory mapSuperType(TypeToken<T> superType, Function<JsonObject, TypeToken<? extends T>> subTypeSelector) {
    return mapSuperType(superType, new JsonObjectDeserializer<T>() {
      @Override
      public T deserialize(JsonObject json, Gson gson, TypeAdapterFactory toSkip) {
        final TypeToken<? extends T> subType = subTypeSelector.apply(json);
        final TypeAdapter<? extends T> adapter = gson.getDelegateAdapter(toSkip, subType);
        return adapter.fromJsonTree(json);
      }

      @Override
      public String toString() {
        return subTypeSelector.toString();
      }
    });
  }

  public static TypeAdapterFactory registerMixin(Class<?> type, Class<?> mixin) {
    return new MixinTypeAdapterFactory(type, mixin);
  }

  public static <T> TypeAdapterFactory postSerialization(TypeToken<T> type, BiConsumer<T, JsonElement> finisher) {
    requireNonNull(type);
    requireNonNull(finisher);
    return new DelegateTypeAdapterFactory<T>(type) {
      @Override
      public String toString() {
        return "Factory[type=" + type + ",adapter=postSerialization(" + finisher + ")]";
      }

      @Override
      public TypeAdapter<T> create(Gson gson) {
        final TypeAdapterFactory that = this;
        final TypeAdapter<JsonElement> jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
        return new TypeAdapter<T>() {
          @SuppressWarnings("unchecked")
          @Override
          public void write(JsonWriter out, T value) throws IOException {
            final TypeAdapter<T> adapter = gson.getDelegateAdapter(that, TypeToken.get((Class<T>) value.getClass()));
            final JsonElement tree = adapter.toJsonTree(value);
            finisher.accept(value, tree);
            jsonElementTypeAdapter.write(out, tree);
          }

          @Override
          public T read(JsonReader in) throws IOException {
            return gson.getDelegateAdapter(that, type).read(in);
          }

          @Override
          public String toString() {
            return finisher.toString();
          }
        };
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static <E> TypeToken<E> wildTypeToken() {
    return (TypeToken<E>) WILD_TYPE_TOKEN;
  }

  public static <T> boolean isWildTypeToken(TypeToken<T> token) {
    return WILD_TYPE_TOKEN.equals(token);
  }
}
