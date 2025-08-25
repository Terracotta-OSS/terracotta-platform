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
package org.terracotta.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.json.Json.Module;
import org.terracotta.json.gson.GsonFactory;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.util.DirectedGraph;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

/**
 * Used to build a Json mapper
 *
 * @author Mathieu Carbou
 */
public class DefaultJsonFactory implements Json.Factory {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJsonFactory.class);

  private final boolean pretty;
  private final Collection<Module> modules;
  private final ClassLoader classLoader;

  public DefaultJsonFactory() {
    this(Arrays.asList(new TerracottaJsonModule(), new JdkJsonModule(), new Jsr310JsonModule()), false, Json.class.getClassLoader());
  }

  private DefaultJsonFactory(Collection<Module> modules, boolean pretty, ClassLoader classLoader) {
    LOGGER.trace("DefaultJsonFactory({}, {}, {})", modules, pretty, classLoader);
    this.pretty = pretty;
    this.modules = new LinkedHashSet<>(modules);
    this.classLoader = classLoader;
  }

  @Override
  public Json create() {
    return new GsonJson(createMapper());
  }

  @Override
  public DefaultJsonFactory pretty() {
    return pretty(true);
  }

  @Override
  public DefaultJsonFactory pretty(boolean pretty) {
    return new DefaultJsonFactory(modules, pretty, classLoader);
  }

  @Override
  public DefaultJsonFactory withClassLoader(ClassLoader classLoader) {
    return new DefaultJsonFactory(modules, pretty, classLoader);
  }

  @Override
  public DefaultJsonFactory withModule(Module module) {
    return withModules(singletonList(module));
  }

  @Override
  public DefaultJsonFactory withModules(Collection<Module> modules) {
    LOGGER.trace("withModules({})", modules);
    final List<Module> newList = new ArrayList<>(this.modules);
    final Collection<Class<? extends Module>> classes = newList.stream().map(Module::getClass).collect(toSet());
    for (Module module : modules) {
      final Class<? extends Module> type = module.getClass();
      if (classes.stream().anyMatch(Predicate.isEqual(type))) {
        throw new IllegalArgumentException("Module with type: " + type + " already added");
      } else {
        classes.add(type);
        newList.add(module);
      }
    }
    return new DefaultJsonFactory(newList, pretty, classLoader);
  }

  public Gson createMapper() {
    LOGGER.trace("createMapper()");

    // resolve module dependencies and override
    final List<GsonModule> gsonModules = resolveModules().stream()
        .filter(GsonModule.class::isInstance)
        .map(GsonModule.class::cast)
        .collect(toList());
    LOGGER.trace("resolveModules(): {}", gsonModules);

    final GsonFactory gsonFactory = new GsonFactory(classLoader, pretty, gsonModules);

    return gsonFactory.create();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        ", pretty=" + pretty +
        ", modules=" + modules +
        '}';
  }

  private List<Module> resolveModules() {
    // modules and classes
    final Map<Class<? extends Module>, Module> resolvedModules = this.modules.stream().collect(toMap(
        Module::getClass,
        identity(),
        (module, module2) -> {
          throw new AssertionError(); // bug here, this should not happen if we correctly handle duplicates
        },
        LinkedHashMap::new));

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Configured modules:" + lineSeparator() + " - " + resolvedModules.keySet().stream().map(Object::toString).collect(joining(lineSeparator() + " - ")));
    }

    // load and add module dependencies
    new ArrayList<>(resolvedModules.values())
        .stream()
        .map(Module::getClass)
        .flatMap(DefaultJsonFactory::withDependencies) // recursive
        .filter(type -> !resolvedModules.containsKey(type)) // avoid duplicates
        .map(type -> {
          try {
            return type.newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .forEach(module -> {
          if (resolvedModules.put(module.getClass(), module) != null) {
            throw new AssertionError(); // bug here, this should not happen if we correctly handle duplicates
          }
        });

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Resolved modules and dependencies:" + lineSeparator() + " - " + resolvedModules.keySet().stream().map(Object::toString).collect(joining(lineSeparator() + " - ")));
    }

    // create graph
    final DirectedGraph<Class<? extends Module>> graph = new DirectedGraph<>();
    graph.addVertex(RootModule.class);
    resolvedModules.keySet().forEach(type -> {
      graph.addEdge(RootModule.class, type);
      // we only override if the module to override has to be loaded.
      getOverrides(type).filter(resolvedModules::containsKey).forEach(overridden -> graph.addEdge(type, overridden));
      getDependencies(type).forEach(dependency -> graph.addEdge(dependency, type));
    });

    LOGGER.trace("Graph:{}{}", lineSeparator(), graph);

    final List<Module> modules = graph.depthFirstTraversal(RootModule.class).map(resolvedModules::get).collect(toList());
    Collections.reverse(modules);
    return modules;

  }

  private static Stream<Class<? extends Module>> getDependencies(Class<? extends Module> moduleType) {
    return concat(
        moduleType == RootModule.class ? empty() : of(RootModule.class), // always first
        Optional.ofNullable(moduleType.getAnnotation(Module.Dependencies.class))
            .map(Module.Dependencies::value)
            .map(Stream::of)
            .orElse(Stream.empty()));
  }

  private static Stream<Class<? extends Module>> getOverrides(Class<? extends Module> moduleType) {
    return Optional.ofNullable(moduleType.getAnnotation(Module.Overrides.class))
        .map(Module.Overrides::value)
        .map(Stream::of)
        .orElse(Stream.empty());
  }

  private static Stream<Class<? extends Module>> withDependencies(Class<? extends Module> moduleType) {
    return concat(of(moduleType), getDependencies(moduleType).flatMap(DefaultJsonFactory::withDependencies));
  }

  private static class GsonJson implements Json {
    private final Gson mapper;

    protected GsonJson(Gson mapper) {
      this.mapper = mapper;
    }

    @Override
    public Map<String, Object> parseObject(Reader r) {
      return mapper.fromJson(r, new TypeToken<Map<String, Object>>() {});
    }

    @Override
    public List<Object> parseList(Reader r) {
      return mapper.fromJson(r, new TypeToken<List<Object>>() {});
    }

    @Override
    public <T> T parse(Reader r, Class<T> type) {
      return mapper.fromJson(r, type);
    }

    @Override
    public Object parse(Reader r, Type type) {
      return mapper.fromJson(r, type);
    }

    @Override
    public String toString(Object o) {
      return mapper.toJson(o);
    }
  }

  // Root module is a placeholder used to resolve the graph and ordering
  static final class RootModule implements Json.Module {
  }
}
