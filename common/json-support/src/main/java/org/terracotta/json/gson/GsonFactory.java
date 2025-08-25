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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.Expose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.json.Json;
import org.terracotta.json.gson.internal.HierarchyTypeAdapterFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class GsonFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(GsonFactory.class);

  private final ClassLoader classLoader;
  private final boolean pretty;
  private final List<GsonModule> gsonModules;

  public GsonFactory(ClassLoader classLoader, boolean pretty, List<GsonModule> gsonModules) {
    this.classLoader = classLoader;
    this.pretty = pretty;
    this.gsonModules = new ArrayList<>(gsonModules);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Gson create() {
    final GsonBuilder builder = new GsonBuilder();

    // IMPORTANT: please read this - how Gson factory ordering works...
    //
    // Factory registration in Gson is important: when an adapter is requested, a factory can be skipped to use the next one for the type.
    // factories are registered in a GsonBuilder, but when creating the Gson instance, Gson is reversing the factory list.
    // Example: we have factoryA and factoryB both factories applicable to the same type X.
    // We call registerTypeAdapterFactory(factoryA)
    // We call registerTypeAdapterFactory(factoryB)
    // Gson will reverse the list and store inside it: [factoryB, factoryA]
    // When we request a type adapter for the type X, factoryB is used. And it can skip itself to go to the next one, which is factory A
    // So the last factory registered has the priority
    //
    // Now, with our module system with dependencies and override, the module list comes sorted:
    // the first module contains the factories that should be looked first.
    //
    // So we need to call configure on those modules, extract the factories, in the registration order,
    // and once we have our list ordered correctly, we need to reverse it and Gson will reverse it again

    // configure modules
    // modules are ordered by dependency / override
    final List<GsonConfig> configs = gsonModules.stream().map(gsonModule -> {
      GsonConfig config = new GsonConfig(builder);
      LOGGER.trace("configure(): {}", gsonModule);
      gsonModule.configure(config);
      return config;
    }).collect(toList());

    // collected factories
    final List<TypeAdapterFactory> factories = new ArrayList<>();

    // add important factories first:
    // - allowed classes
    // - sorting
    // - force writing null

    // handle support for allowed classes (for security reasons)
    // this must be registered first because it exposes the UnsafeClassSupport
    // and no other adapter should handle Class types before it
    Stream.of(configs.stream()
            .map(GsonConfig::getAllowedClasses)
            .flatMap(Collection::stream)
            .collect(toCollection(TreeSet::new)))
        .filter(types -> !types.isEmpty())
        .map(allowedTypes -> Adapters.allowClassLoading(classLoader, allowedTypes))
        .peek(factory -> LOGGER.trace("+factory: {}", factory))
        .forEach(factories::add);

    Stream.of(
            Adapters.writeNull(Json.Null.class),
            Adapters.SORT_KEYS
        )
        .peek(factory -> LOGGER.trace("+factory: {}", factory))
        .forEach(factories::add);

    // handle support for unsafe deserialization
    configs.stream()
        .map(GsonConfig::getUnsafeTypes)
        .flatMap(Collection::stream)
        .collect(toSet())
        .stream()
        .map(Adapters::registerUnsafeTypeAdapter)
        .peek(factory -> LOGGER.trace("+factory: {}", factory))
        .forEach(factories::add);

    // extract factories and type hierarchy
    final Map<Class<?>, HierarchyTypeAdapterFactory<?>> superTypes = new LinkedHashMap<>();
    for (GsonConfig config : configs) {
      // grab factories from the modules, in resolved order
      config.factories()
          .peek(factory -> LOGGER.trace("+factory: {}", factory))
          .peek(factory -> {
            if (factory instanceof HierarchyTypeAdapterFactory<?>) {
              final HierarchyTypeAdapterFactory<?> hierarchyTypeAdapterFactory = (HierarchyTypeAdapterFactory<?>) factory;
              if (superTypes.put(hierarchyTypeAdapterFactory.getBaseType(), hierarchyTypeAdapterFactory) != null) {
                throw new IllegalStateException("Duplicate " + HierarchyTypeAdapterFactory.class.getSimpleName() + " found for base type: " + hierarchyTypeAdapterFactory.getBaseType());
              }
            }
          })
          .forEach(factories::add);
    }

    // add subtypes
    configs.stream()
        .flatMap(config -> config.getSubTypes().entrySet().stream())
        .forEach(e -> {
          final Class<?> superType = e.getKey();
          final HierarchyTypeAdapterFactory<?> factory = superTypes.get(superType);
          if (factory == null) {
            throw new IllegalStateException(HierarchyTypeAdapterFactory.class.getSimpleName() + " missing for super type: " + superType);
          }
          e.getValue().forEach((subType, label) -> factory.withSubtype((Class) subType.asSubclass(superType), label));
        });

    // set build settings which cannot be overridden

    builder.disableHtmlEscaping();
    builder.disableJdkUnsafe();

    if (pretty) {
      builder.setPrettyPrinting();
    }

    // reduce the scope of Gson reflection capabilities
    Adapters.DEFAULT_ACCESS_FILTERS.forEach(builder::addReflectionAccessFilter);

    // add support for @Expose in mixins
    builder.addDeserializationExclusionStrategy(new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes f) {
        final Expose annotation = f.getAnnotation(Expose.class);
        return annotation != null && !annotation.deserialize();
      }

      @Override
      public boolean shouldSkipClass(Class<?> clazz) {
        return false;
      }
    });

    // add support for @Expose in mixins
    builder.addSerializationExclusionStrategy(new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes f) {
        final Expose annotation = f.getAnnotation(Expose.class);
        return annotation != null && !annotation.serialize();
      }

      @Override
      public boolean shouldSkipClass(Class<?> clazz) {
        return false;
      }
    });

    // finally, reverse our factory list and register it
    Collections.reverse(factories);
    factories.forEach(builder::registerTypeAdapterFactory);

    return builder.create();
  }
}
