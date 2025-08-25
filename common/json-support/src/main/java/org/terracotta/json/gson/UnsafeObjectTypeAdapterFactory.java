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

import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.terracotta.json.gson.internal.UnsafeClassSupport;
import org.terracotta.json.gson.internal.Utils;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/**
 * Type adapter that can be used to map a field without knowing its type in advance,
 * by adding a class information in the serialized json.
 * <p>
 * Since deserialization requires to load a class, this adapter is unsafe and should only be
 * used on a very controlled set of classes.
 * <p>
 * Gson is strongly typed and does not allow the serialisation of an object if it doesn't know how to deserialize it.
 * Gson uses the declared type to determine how to deserialize.
 * But some classes do not declare a specific type (i.e. raw collections, raw types like Object or Object[]).
 * For such cases, jackson was automatically appending the `@class` information next to the serialized data and was doing a classloader call when deserializing.
 * Gson does not support this, because it prevents serialising something "unknown", and does not support class loading (one of the reason why there is so few security issues with Gson).
 * So this class allows to mark some types or annotated fields on mixins to support adding the class information next to the serialized object exactly like Jackson was doing.
 * <p>
 * This class can be used on a mixin with {@link com.google.gson.annotations.JsonAdapter}
 */
public class UnsafeObjectTypeAdapterFactory<T> implements TypeAdapterFactory {
  private final TypeToken<T> declaredTypeToken;
  private final String typeLabel;
  private final boolean sortKeys;
  private final ConstructorConstructor constructorConstructor;

  // This construction is called when this class is used as an annotation placed on a field with type Object.
  private UnsafeObjectTypeAdapterFactory() {
    this(Adapters.wildTypeToken(), "@class", true, emptyMap());
  }

  public UnsafeObjectTypeAdapterFactory(TypeToken<T> declaredType) {
    this(declaredType, "@class", true, emptyMap());
  }

  private UnsafeObjectTypeAdapterFactory(TypeToken<T> declaredType,
                                         String typeLabel,
                                         boolean sortKeys,
                                         Map<Type, InstanceCreator<?>> instanceCreators) {
    this(declaredType, typeLabel, sortKeys, new ConstructorConstructor(instanceCreators, false, Adapters.DEFAULT_ACCESS_FILTERS));
  }

  private UnsafeObjectTypeAdapterFactory(TypeToken<T> declaredType,
                                         String typeLabel,
                                         boolean sortKeys,
                                         ConstructorConstructor constructorConstructor) {
    this.declaredTypeToken = requireNonNull(declaredType);
    this.typeLabel = requireNonNull(typeLabel);
    this.sortKeys = sortKeys;
    this.constructorConstructor = constructorConstructor;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
    if (declaredTypeToken.getRawType() == Object.class || declaredTypeToken.equals(type)) {
      return (TypeAdapter<R>) new UnsafeObjectTypeAdapter(gson);
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return "Factory[type=" + declaredTypeToken + ",adapter=" + UnsafeObjectTypeAdapter.class.getName() + "]";
  }

  private boolean isWildTypeToken() {
    return Adapters.isWildTypeToken(declaredTypeToken);
  }

  @SuppressWarnings("unchecked")
  private <E> JsonElement serializeArray(Gson gson, TypeToken<E[]> token, Object value) {
    final int max = Array.getLength(value);
    final JsonArray array = new JsonArray(max);
    final Class<E> componentType = (Class<E>) token.getRawType().getComponentType();
    final TypeToken<E> componentTypeToken = TypeToken.get(componentType);
    final TypeAdapter<E> componentAdapter = new UnsafeObjectTypeAdapterFactory<>(componentTypeToken, typeLabel, sortKeys, constructorConstructor).create(gson, componentTypeToken);
    for (int i = 0; i < max; i++) {
      E element = (E) Array.get(value, i);
      array.add(componentAdapter.toJsonTree(element));
    }
    return array;
  }

  @SuppressWarnings("unchecked")
  private <E> JsonElement serializeCollection(Gson gson, Object value) {
    final Collection<E> collection = (Collection<E>) value;
    final int max = collection.size();
    final JsonArray array = new JsonArray(max);
    final TypeAdapter<E> componentAdapter = new UnsafeObjectTypeAdapterFactory<>(Adapters.wildTypeToken(), typeLabel, sortKeys, constructorConstructor).create(gson, Adapters.wildTypeToken());
    for (E element : collection) {
      array.add(componentAdapter.toJsonTree(element));
    }
    return array;
  }

  @SuppressWarnings("unchecked")
  private <E> E[] deserializeArray(Gson gson, JsonArray jsonArray, TypeToken<E[]> token) {
    final TypeToken<E> componentTypeToken = (TypeToken<E>) TypeToken.get(token.getRawType().getComponentType());
    final TypeAdapter<E> componentAdapter = new UnsafeObjectTypeAdapterFactory<>(componentTypeToken, typeLabel, sortKeys, constructorConstructor).create(gson, componentTypeToken);
    final int max = jsonArray.size();
    final E[] result = (E[]) Array.newInstance(componentTypeToken.getRawType(), max);
    for (int i = 0; i < max; i++) {
      Array.set(result, i, componentAdapter.fromJsonTree(jsonArray.get(i)));
    }
    return result;
  }

  private <E> Collection<E> deserializeCollection(Gson gson, JsonArray jsonArray, TypeToken<Collection<E>> token) {
    final TypeAdapter<E> componentAdapter = new UnsafeObjectTypeAdapterFactory<>(Adapters.wildTypeToken(), typeLabel, sortKeys, constructorConstructor).create(gson, Adapters.wildTypeToken());
    Collection<E> collection = constructorConstructor.get(token).construct();
    final int max = jsonArray.size();
    for (int i = 0; i < max; i++) {
      collection.add(componentAdapter.fromJsonTree(jsonArray.get(i)));
    }
    return collection;
  }

  @SuppressWarnings("unchecked")
  private Optional<TypeToken<T>> findRuntimeType(UnsafeClassSupport unsafeClassSupport, JsonArray jsonArray) {
    // wrapped array ?
    if (jsonArray.size() == 2
        && jsonArray.get(0).isJsonPrimitive()
        && jsonArray.get(0).getAsJsonPrimitive().isString()
        && unsafeClassSupport.isAllowed(jsonArray.get(0).getAsJsonPrimitive().getAsString())) {
      final String cname = jsonArray.get(0).getAsJsonPrimitive().getAsString();
      final Class<?> runtimeType = unsafeClassSupport.load(cname);
      final JsonElement runtimeElement = jsonArray.get(1);
      if (!declaredTypeToken.getRawType().isAssignableFrom(runtimeType)) {
        throw new IllegalStateException("Unable to deserialize json: " + runtimeElement + " with type: " + runtimeType + " into declared type: " + declaredTypeToken);
      }
      return Optional.of((TypeToken<T>) TypeToken.get(runtimeType));
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private Optional<TypeToken<T>> findRuntimeType(UnsafeClassSupport unsafeClassSupport, JsonObject jsonObject) {
    // object with type inside ?
    if (jsonObject.has(typeLabel)) {
      final JsonElement typeLabelEl = jsonObject.get(typeLabel);
      if (typeLabelEl.isJsonPrimitive() && typeLabelEl.getAsJsonPrimitive().isString()) {
        final String cname = typeLabelEl.getAsString();
        if (!unsafeClassSupport.isAllowed(cname)) {
          throw new IllegalStateException("Unauthorized to load class name: " + cname + " to deserialize json object: " + jsonObject);
        }
        final Class<?> runtimeType = unsafeClassSupport.load(cname);
        if (!declaredTypeToken.getRawType().isAssignableFrom(runtimeType)) {
          throw new IllegalStateException("Unable to deserialize json: " + jsonObject + " with type: " + runtimeType + " into declared type: " + declaredTypeToken);
        }
        return Optional.of((TypeToken<T>) TypeToken.get(runtimeType));
      }
    }
    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private static <E> TypeToken<E> getRuntimeTypeIfMoreSpecific(TypeToken<E> typeToken, E value) {
    if (value != null && typeToken.getRawType().isAssignableFrom(value.getClass())) {
      return (TypeToken<E>) TypeToken.get(value.getClass());
    }
    return typeToken;
  }

  private static String getName(Class<?> clazz) {
    final Class<?> superclass = clazz.getSuperclass();
    if (superclass != null && superclass.isEnum()) {
      return superclass.getName();
    } else {
      return clazz.getName();
    }
  }

  private static UnsafeClassSupport getUnsafeClassSupport(Gson gson) {
    final TypeAdapter<Class<?>> adapter = gson.getAdapter(new TypeToken<Class<?>>() {});
    return adapter instanceof UnsafeClassSupport ? (UnsafeClassSupport) adapter : UnsafeClassSupport.ALLOW_NONE;
  }

  private static boolean isWildCollection(TypeToken<?> token) {
    return Collection.class.isAssignableFrom(token.getRawType())
        && token.getType() == token.getRawType()
        || token.getType() instanceof ParameterizedType
        && TypeToken.get(((ParameterizedType) token.getType()).getActualTypeArguments()[0]).getRawType() == Object.class;
  }

  private static boolean isWildArray(TypeToken<?> token) {
    return token.getType() instanceof GenericArrayType
        && ((GenericArrayType) token.getType()).getGenericComponentType() instanceof Class;
  }

  private static JsonElement wrap(String cname, JsonElement jsonElement) {
    final JsonArray wrapper = new JsonArray();
    wrapper.add(cname);
    wrapper.add(jsonElement);
    return wrapper;
  }

  private class UnsafeObjectTypeAdapter extends TypeAdapter<T> {
    private final Gson gson;
    private final TypeAdapter<JsonElement> jsonElementTypeAdapter;

    public UnsafeObjectTypeAdapter(Gson gson) {
      this.gson = gson;
      this.jsonElementTypeAdapter = gson.getAdapter(JsonElement.class);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + ":" + declaredTypeToken;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T read(JsonReader in) throws IOException {
      final JsonElement jsonElement = jsonElementTypeAdapter.read(in);

      if (jsonElement.isJsonNull()) {
        return null;
      }

      if (jsonElement.isJsonObject()) {
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final TypeToken<T> runtimeType = findRuntimeType(getUnsafeClassSupport(gson), jsonObject).orElse(declaredTypeToken);
        return gson.fromJson(jsonObject, runtimeType);
      }

      if (jsonElement.isJsonArray()) {
        final JsonArray jsonArray = jsonElement.getAsJsonArray();
        return findRuntimeType(getUnsafeClassSupport(gson), jsonArray)
            .map(runtimeType -> {
              final TypeAdapter<T> adapter = new UnsafeObjectTypeAdapterFactory<>(runtimeType, typeLabel, sortKeys, constructorConstructor).create(gson, runtimeType);
              return adapter.fromJsonTree(jsonArray.get(1));
            })
            .orElseGet(() -> {
              if (isWildArray(declaredTypeToken)) {
                return (T) deserializeArray(gson, jsonArray, (TypeToken) declaredTypeToken);
              }
              if (isWildCollection(declaredTypeToken)) {
                return (T) deserializeCollection(gson, jsonArray, (TypeToken) declaredTypeToken);
              }
              throw new IllegalArgumentException("Unable to deserialize array: " + jsonArray);
            });
      }

      if (jsonElement.isJsonPrimitive()) {

        if (declaredTypeToken.getRawType() != Object.class) {
          return gson.fromJson(jsonElement, declaredTypeToken);
        }

        JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();

        if (jsonPrimitive.isString()) {
          return (T) jsonPrimitive.getAsString();
        }
        if (jsonPrimitive.isBoolean()) {
          return (T) (Boolean) jsonPrimitive.getAsBoolean();
        }
        if (jsonPrimitive.isNumber()) {
          // quite hacky but uses our bets effort to deserialize numbers when no type is given
          final JsonReader reader = new JsonReader(new StringReader(jsonPrimitive.getAsString()));
          return (T) Adapters.JACKSON_LIKE_NUMBER_STRATEGY.readNumber(reader);
        }
      }

      throw new AssertionError();
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      final JsonElement jsonElement = serialize(value);
      jsonElementTypeAdapter.write(out, jsonElement);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private JsonElement serialize(T value) {
      if (value instanceof Optional) {
        value = (T) ((Optional<?>) value).orElse(null);
      }

      if (value == null) {
        return JsonNull.INSTANCE;
      }

      final TypeToken<T> runtimeType = getRuntimeTypeIfMoreSpecific(declaredTypeToken, value);
      final String cname = getName(runtimeType.getRawType());

      if (isWildArray(runtimeType)) {
        return isWildTypeToken() ?
            wrap(cname, serializeArray(gson, (TypeToken) runtimeType, value)) :
            serializeArray(gson, (TypeToken) runtimeType, value);
      }

      if (isWildCollection(runtimeType)) {
        return isWildTypeToken() || declaredTypeToken.getRawType().isInterface() || Modifier.isAbstract(declaredTypeToken.getRawType().getModifiers()) ?
            wrap(cname, serializeCollection(gson, value)) :
            serializeCollection(gson, value);
      }

      final JsonElement jsonElement = declaredTypeToken.equals(runtimeType) ?
          gson.getAdapter(declaredTypeToken).toJsonTree(value) :
          gson.getAdapter(runtimeType).toJsonTree(value);

      if (jsonElement.isJsonObject()) {
        if (isWildTypeToken()) {
          jsonElement.getAsJsonObject().addProperty(typeLabel, cname);
        }
        return sortKeys ? Utils.sort(jsonElement) : jsonElement;
      }

      if (jsonElement.isJsonPrimitive() && (value instanceof CharSequence || value instanceof Number || value instanceof Boolean)) {
        return jsonElement;
      }

      return wrap(cname, jsonElement);
    }
  }
}
