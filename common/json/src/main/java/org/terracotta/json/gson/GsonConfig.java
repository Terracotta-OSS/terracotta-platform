/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import org.terracotta.json.Json;
import org.terracotta.json.gson.internal.HierarchyTypeAdapterFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class GsonConfig {
  private final com.google.gson.GsonBuilder builder;
  private final Map<Class<?>, HierarchyTypeAdapterFactory<?>> superTypes = new IdentityHashMap<>(0);
  private final Map<Class<?>, Map<Class<?>, String>> subTypes = new IdentityHashMap<>(0);
  private final Collection<TypeAdapterFactory> factories = new ArrayList<>(0);
  private final Collection<String> allowedClasses = new HashSet<>();
  private final Collection<TypeToken<?>> unsafeTypes = new HashSet<>();

  public GsonConfig(com.google.gson.GsonBuilder builder) {
    this.builder = builder;
  }

  public com.google.gson.GsonBuilder getBuilder() {
    return builder;
  }

  Stream<TypeAdapterFactory> factories() {
    return Stream.concat(factories.stream(), superTypes.values().stream());
  }

  Map<Class<?>, Map<Class<?>, String>> getSubTypes() {
    return subTypes;
  }

  Collection<String> getAllowedClasses() {
    return allowedClasses;
  }

  Collection<TypeToken<?>> getUnsafeTypes() {
    return unsafeTypes;
  }

  /**
   * @see TypeAdapters#newFactory(Class, TypeAdapter)
   */
  public final <T> void registerTypeAdapter(Class<T> type, TypeAdapter<T> adapter) {
    factories.add(TypeAdapters.newFactory(type, Adapters.nullSafe(adapter)));
  }

  /**
   * @see TypeAdapters#newFactory(TypeToken, TypeAdapter)
   */
  public final <T> void registerTypeAdapter(TypeToken<T> type, TypeAdapter<T> adapter) {
    factories.add(TypeAdapters.newFactory(type, Adapters.nullSafe(adapter)));
  }

  /**
   * @see Adapters#registerUnsafeTypeAdapter(TypeToken)
   */
  public final void registerUnsafeTypeAdapters(Class<?>... types) {
    Stream.of(types).map(TypeToken::get).forEach(unsafeTypes::add);
  }

  /**
   * @see Adapters#registerUnsafeTypeAdapter(TypeToken)
   */
  public final void registerUnsafeTypeAdapters(TypeToken<?>... types) {
    unsafeTypes.addAll(asList(types));
  }

  /**
   * @see TypeAdapters#newTypeHierarchyFactory(Class, TypeAdapter)
   */
  public final <T> void registerTypeHierarchyAdapter(Class<T> baseType, TypeAdapter<T> typeAdapter) {
    factories.add(TypeAdapters.newTypeHierarchyFactory(baseType, Adapters.nullSafe(typeAdapter)));
  }

  /**
   * @see TypeAdapters#newTypeHierarchyFactory(Class, TypeAdapter)
   */
  @SuppressWarnings("unchecked")
  public final <T> void registerTypeHierarchyAdapter(TypeToken<T> baseType, TypeAdapter<T> typeAdapter) {
    factories.add(TypeAdapters.newTypeHierarchyFactory((Class<T>) baseType.getRawType(), Adapters.nullSafe(typeAdapter)));
  }

  public void registerTypeAdapterFactory(TypeAdapterFactory factory) {
    factories.add(factory);
  }

  /**
   * @see Adapters#writeNull(Class)
   */
  public final <T> void writeNull(Class<T> type) {
    factories.add(Adapters.writeNull(type));
  }

  /**
   * @see Adapters#objectToString(TypeToken, Function)
   */
  public final <T> void objectToString(Class<T> type, Function<String, T> reverse) {
    factories.add(Adapters.objectToString(TypeToken.get(type), reverse));
  }

  /**
   * @see Adapters#objectToString(TypeToken, Function)
   */
  public final <T> void objectToString(TypeToken<T> type, Function<String, T> reverse) {
    factories.add(Adapters.objectToString(type, reverse));
  }

  /**
   * @see Adapters#objectToString(TypeToken, Function, Function)
   */
  public final <T> void objectToString(Class<T> type, Function<T, String> stringifier, Function<String, T> reverse) {
    factories.add(Adapters.objectToString(TypeToken.get(type), stringifier, reverse));
  }

  /**
   * @see Adapters#objectToString(TypeToken, Function, Function)
   */
  public final <T> void objectToString(TypeToken<T> type, Function<T, String> stringifier, Function<String, T> reverse) {
    factories.add(Adapters.objectToString(type, stringifier, reverse));
  }

  /**
   * @see Adapters#objectToLong(TypeToken, Function, Function)
   */
  public final <T> void objectToLong(Class<T> type, Function<T, Long> longifier, Function<Long, T> reverse) {
    factories.add(Adapters.objectToLong(TypeToken.get(type), longifier, reverse));
  }

  /**
   * @see Adapters#objectToLong(TypeToken, Function, Function)
   */
  public final <T> void objectToLong(TypeToken<T> type, Function<T, Long> longifier, Function<Long, T> reverse) {
    factories.add(Adapters.objectToLong(type, longifier, reverse));
  }

  /**
   * @see Adapters#allowClassLoading(ClassLoader, Collection)
   * @see Json.Factory#withClassLoader(ClassLoader)
   */
  public final void allowClassLoading(Class<?>... allowed) {
    Stream.of(allowed).map(Class::getName).forEach(allowedClasses::add);
  }

  /**
   * @see Adapters#allowClassLoading(ClassLoader, Collection)
   * @see Json.Factory#withClassLoader(ClassLoader)
   */
  public final void allowClassLoading(String... allowed) {
    allowedClasses.addAll(asList(allowed));
  }

  /**
   * @see Adapters#serializeSubtypes(TypeToken)
   */
  public final <T> void serializeSubtypes(Class<T> superType) {
    factories.add(Adapters.serializeSubtypes(TypeToken.get(superType)));
  }

  /**
   * @see Adapters#serializeSubtypes(TypeToken)
   */
  public final <T> void serializeSubtypes(TypeToken<T> superType) {
    factories.add(Adapters.serializeSubtypes(superType));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, JsonObjectDeserializer)
   */
  public final <T> void mapSuperType(Class<T> type, JsonObjectDeserializer<T> deserializer) {
    factories.add(Adapters.mapSuperType(TypeToken.get(type), deserializer));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, JsonObjectDeserializer)
   */
  public final <T> void mapSuperType(TypeToken<T> type, JsonObjectDeserializer<T> deserializer) {
    factories.add(Adapters.mapSuperType(type, deserializer));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, TypeToken)
   */
  public final <T> void mapSuperType(Class<T> superType, Class<? extends T> subType) {
    factories.add(Adapters.mapSuperType(TypeToken.get(superType), TypeToken.get(subType)));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, TypeToken)
   */
  public final <T> void mapSuperType(TypeToken<T> superType, TypeToken<? extends T> subType) {
    factories.add(Adapters.mapSuperType(superType, subType));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, Function)
   */
  public final <T> void mapSuperType(Class<T> superType, Function<JsonObject, Class<? extends T>> subTypeSelector) {
    factories.add(Adapters.mapSuperType(TypeToken.get(superType), new Function<JsonObject, TypeToken<? extends T>>() {
      @Override
      public TypeToken<? extends T> apply(JsonObject jsonObject) {
        return TypeToken.get(subTypeSelector.apply(jsonObject));
      }

      @Override
      public String toString() {
        return subTypeSelector.toString();
      }
    }));
  }

  /**
   * @see Adapters#mapSuperType(TypeToken, Function)
   */
  public final <T> void mapSuperType(TypeToken<T> superType, Function<JsonObject, TypeToken<? extends T>> subTypeSelector) {
    factories.add(Adapters.mapSuperType(superType, subTypeSelector));
  }

  /**
   * @see Adapters#registerSuperType(Class)
   */
  public final <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> type) {
    final HierarchyTypeAdapterFactory<T> factory = Adapters.registerSuperType(type);
    if (superTypes.put(type, factory) != null) {
      throw new IllegalStateException("Duplicate registration for type: " + type);
    }
    return factory;
  }

  /**
   * @see Adapters#registerSuperType(Class, String)
   */
  public final <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> type, String typeFieldName) {
    final HierarchyTypeAdapterFactory<T> factory = Adapters.registerSuperType(type, typeFieldName);
    if (superTypes.put(type, factory) != null) {
      throw new IllegalStateException("Duplicate registration for type: " + type);
    }
    return factory;
  }

  /**
   * @see Adapters#registerSuperType(Class, String, Function)
   */
  public final <T> HierarchyTypeAdapterFactory<T> registerSuperType(Class<T> type, String typeFieldName, Function<Class<? extends T>, String> mapper) {
    final HierarchyTypeAdapterFactory<T> factory = Adapters.registerSuperType(type, typeFieldName, mapper);
    if (superTypes.put(type, factory) != null) {
      throw new IllegalStateException("Duplicate registration for type: " + type);
    }
    return factory;
  }

  /**
   * @see HierarchyTypeAdapterFactory#withSubtype(Class)
   */
  public final <T> void registerSubtype(Class<T> superType, Class<? extends T> subType) {
    registerSubtype(superType, subType, subType.getSimpleName());
  }

  /**
   * @see HierarchyTypeAdapterFactory#withSubtypes(Class[])
   */
  @SuppressWarnings("unchecked")
  public final <T> void registerSubtypes(Class<T> superType, Class<? extends T>... subTypes) {
    for (Class<? extends T> subType : subTypes) {
      registerSubtype(superType, subType);
    }
  }

  /**
   * @see HierarchyTypeAdapterFactory#withSubtype(Class, String)
   */
  @SuppressWarnings("unchecked")
  public final <T> void registerSubtype(Class<T> superType, Class<? extends T> subType, String label) {
    final HierarchyTypeAdapterFactory<T> factory = (HierarchyTypeAdapterFactory<T>) superTypes.get(superType);
    if (factory != null) {
      factory.withSubtype(subType, label);
    } else {
      subTypes.computeIfAbsent(superType, st -> new IdentityHashMap<>(1)).put(subType, label);
    }
  }

  /**
   * @see Adapters#registerMixin(Class, Class)
   */
  public final void registerMixin(Class<?> type, Class<?> mixin) {
    factories.add(Adapters.registerMixin(type, mixin));
  }

  public <T> void postSerialization(TypeToken<T> type, BiConsumer<T, JsonElement> finisher) {
    factories.add(Adapters.postSerialization(type, finisher));
  }

  public <T> void postSerialization(Class<T> type, BiConsumer<T, JsonElement> finisher) {
    factories.add(Adapters.postSerialization(TypeToken.get(type), finisher));
  }
}
