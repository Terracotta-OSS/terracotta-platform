/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.dynamic_config.server.conversion.helper;

import java.lang.reflect.Field;
import java.util.Optional;

public class ReflectionHelper {

  public static <T> Optional<T> getDeclaredField(Class<T> fieldType, String fieldName
      , Object instance) throws Exception {
    Field field = instance.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    if (fieldType.isAssignableFrom(field.getType())) {
      @SuppressWarnings("unchecked")
      T retFiled = (T)field.get(instance);
      return Optional.of(retFiled);
    }
    return Optional.empty();
  }

  public static <T> Optional<T> getField(Class<T> fieldType, String fieldName, Object instance) throws Exception {
    Field field = null;
    @SuppressWarnings("rawtypes")
    Class clazz = instance.getClass();
    while (clazz != null) {
      try {
        field = clazz.getDeclaredField(fieldName);
        break;
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    if (field == null) {
      return Optional.empty();
    }
    field.setAccessible(true);
    if (fieldType.isAssignableFrom(field.getType())) {
      @SuppressWarnings("unchecked")
      T retFiled = (T)field.get(instance);
      return Optional.of(retFiled);
    }
    return Optional.empty();
  }
}