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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.terracotta.json.gson.Adapters.nullSafe;

/**
 * <pre>
 * NOTE FROM TERRACOTTA DEVS
 *
 * This class has been copied and adapted from the Gson project at:
 * <a href="https://github.com/google/gson/blob/main/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java">https://github.com/google/gson/blob/main/extras/src/main/java/com/google/gson/typeadapters/RuntimeTypeAdapterFactory.java</a>
 * These extra classes are not available in any Maven repo and are meant as samples to be copied and adapted.
 * The original copy was adapted with these modifications included:
 *  - added mapper to map sub classes to string
 *  - fixed maintainType
 *  - added sort
 * </pre>
 *
 * <p>
 * Adapts values whose runtime type may differ from their declaration type. This
 * is necessary when a field's type is not the same type that GSON should create
 * when deserializing that field. For example, consider these types:
 * <pre>   {@code
 *   abstract class Shape {
 *     int x;
 *     int y;
 *   }
 *   class Circle extends Shape {
 *     int radius;
 *   }
 *   class Rectangle extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Diamond extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Drawing {
 *     Shape bottomShape;
 *     Shape topShape;
 *   }
 * }</pre>
 * <p>Without additional type information, the serialized JSON is ambiguous. Is
 * the bottom shape in this drawing a rectangle or a diamond? <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * This class addresses this problem by adding type information to the
 * serialized JSON and honoring that type information when the JSON is
 * deserialized: <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "type": "Diamond",
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "type": "Circle",
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * Both the type field name ({@code "type"}) and the type labels ({@code
 * "Rectangle"}) are configurable.
 * <p>
 * Registering Types
 * <p>
 * Create a {@code RuntimeTypeAdapterFactory} by passing the base type and type field
 * name to the {@link #of} factory method. If you don't supply an explicit type
 * field name, {@code "type"} will be used. <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 *       = RuntimeTypeAdapterFactory.of(Shape.class, "type");
 * }</pre>
 * Next register all of your subtypes. Every subtype must be explicitly
 * registered. This protects your application from injection attacks. If you
 * don't supply an explicit type label, the type's simple name will be used.
 * <pre>   {@code
 *   shapeAdapterFactory.registerSubtype(Rectangle.class, "Rectangle");
 *   shapeAdapterFactory.registerSubtype(Circle.class, "Circle");
 *   shapeAdapterFactory.registerSubtype(Diamond.class, "Diamond");
 * }</pre>
 * Finally, register the type adapter factory in your application's GSON builder:
 * <pre>   {@code
 *   Gson gson = new GsonBuilder()
 *       .registerTypeAdapterFactory(shapeAdapterFactory)
 *       .create();
 * }</pre>
 * Like {@code GsonBuilder}, this API supports chaining: <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 *       .registerSubtype(Rectangle.class)
 *       .registerSubtype(Circle.class)
 *       .registerSubtype(Diamond.class);
 * }</pre>
 * <p>
 * Serialization and deserialization
 * <p>
 * In order to serialize and deserialize a polymorphic object,
 * you must specify the base type explicitly.
 * <pre>   {@code
 *   Diamond diamond = new Diamond();
 *   String json = gson.toJson(diamond, Shape.class);
 * }</pre>
 * And then:
 * <pre>   {@code
 *   Shape shape = gson.fromJson(json, Shape.class);
 * }</pre>
 */
public final class HierarchyTypeAdapterFactory<T> implements TypeAdapterFactory {
  private final Class<?> baseType;
  private final String typeFieldName;
  private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<>();
  private final Map<Class<?>, String> subtypeToLabel = new LinkedHashMap<>();
  private final boolean maintainType;
  private final Function<Class<? extends T>, String> mapper;
  private final boolean sortKeys;
  private boolean recognizeSubtypes;

  private HierarchyTypeAdapterFactory(Class<?> baseType, String typeFieldName, boolean maintainType, boolean sortKeys, Function<Class<? extends T>, String> mapper) {
    requireNonNull(baseType);
    requireNonNull(typeFieldName);
    requireNonNull(mapper);
    this.sortKeys = sortKeys;
    this.baseType = baseType;
    this.typeFieldName = typeFieldName;
    this.maintainType = maintainType;
    this.mapper = mapper;
  }

  /**
   * Creates a new runtime type adapter using for {@code baseType} using {@code
   * typeFieldName} as the type field name. Type field names are case-sensitive.
   *
   * @param maintainType true if the type field should be included in deserialized objects
   */
  public static <T> HierarchyTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName, boolean maintainType, boolean sortKeys, Function<Class<? extends T>, String> mapper) {
    return new HierarchyTypeAdapterFactory<>(baseType, typeFieldName, maintainType, sortKeys, mapper);
  }

  @Override
  public String toString() {
    return "Factory[type=" + baseType + ",subtpes=" + subtypeToLabel.keySet() + "]";
  }

  public Class<?> getBaseType() {
    return baseType;
  }

  /**
   * Ensures that this factory will handle not just the given {@code baseType}, but any subtype
   * of that type.
   */
  public HierarchyTypeAdapterFactory<T> recognizeSubtypes() {
    this.recognizeSubtypes = true;
    return this;
  }

  /**
   * Registers {@code type} identified by {@code label}. Labels are case-sensitive.
   *
   * @throws IllegalArgumentException if either {@code type} or {@code label}
   *                                  have already been registered on this type adapter.
   */
  public HierarchyTypeAdapterFactory<T> withSubtype(Class<? extends T> type, String label) {
    if (type == null || label == null) {
      throw new NullPointerException();
    }
    if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
      throw new IllegalArgumentException("types and labels must be unique");
    }
    labelToSubtype.put(label, type);
    subtypeToLabel.put(type, label);
    return this;
  }

  /**
   * Registers {@code type} identified by its {@link Class#getSimpleName simple
   * name}. Labels are case-sensitive.
   *
   * @throws IllegalArgumentException if either {@code type} or its simple name
   *                                  have already been registered on this type adapter.
   */
  public HierarchyTypeAdapterFactory<T> withSubtype(Class<? extends T> type) {
    return withSubtype(type, mapper.apply(type));
  }

  @SafeVarargs
  public final HierarchyTypeAdapterFactory<T> withSubtypes(Class<? extends T>... types) {
    for (Class<? extends T> t : types) {
      withSubtype(t);
    }
    return this;
  }

  @Override
  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
    if (type == null) {
      return null;
    }
    Class<?> rawType = type.getRawType();
    boolean handle =
        recognizeSubtypes ? baseType.isAssignableFrom(rawType) : baseType.equals(rawType);
    if (!handle) {
      return null;
    }

    final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
    final Map<String, TypeAdapter<?>> labelToDelegate = new LinkedHashMap<>();
    final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();
    for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
      TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
      labelToDelegate.put(entry.getKey(), delegate);
      subtypeToDelegate.put(entry.getValue(), delegate);
    }

    return nullSafe(new TypeAdapter<R>() {
      @Override
      public R read(JsonReader in) throws IOException {
        JsonElement jsonElement = jsonElementAdapter.read(in);
        JsonElement labelJsonElement;
        if (maintainType) {
          labelJsonElement = jsonElement.getAsJsonObject().get(typeFieldName);
        } else {
          labelJsonElement = jsonElement.getAsJsonObject().remove(typeFieldName);
        }

        if (labelJsonElement == null) {
          throw new JsonParseException("cannot deserialize " + baseType
              + " because it does not define a field named " + typeFieldName);
        }
        String label = labelJsonElement.getAsString();
        @SuppressWarnings("unchecked") // registration requires that subtype extends T
        TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
        if (delegate == null) {
          throw new JsonParseException("cannot deserialize " + baseType + " subtype named "
              + label + "; did you forget to register a subtype?");
        }
        return delegate.fromJsonTree(jsonElement);
      }

      @Override
      public void write(JsonWriter out, R value) throws IOException {
        Class<?> srcType = value.getClass();
        String label = subtypeToLabel.get(srcType);
        @SuppressWarnings("unchecked") // registration requires that subtype extends T
        TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
        if (delegate == null) {
          throw new JsonParseException("cannot serialize " + srcType.getName()
              + "; did you forget to register a subtype?");
        }
        JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();

        if (!maintainType) {
          jsonElementAdapter.write(out, sortKeys ? Utils.sort(jsonObject) : jsonObject);
          return;
        }

        JsonObject clone = new JsonObject();

        if (jsonObject.has(typeFieldName)) {
          throw new JsonParseException("cannot serialize " + srcType.getName()
              + " because it already defines a field named " + typeFieldName);
        }
        clone.add(typeFieldName, new JsonPrimitive(label));

        for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
          clone.add(e.getKey(), e.getValue());
        }
        jsonElementAdapter.write(out, sortKeys ? Utils.sort(clone) : clone);
      }
    });
  }
}