/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.migration.helper;

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