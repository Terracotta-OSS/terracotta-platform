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
package org.terracotta.dynamic_config.cli.api.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class Injector {
  public static <T> T inject(T target, Object[] services) {
    return inject(target, asList(services));
  }

  public static <T> T inject(T target, Collection<Object> services) {
    Stream.of(target.getClass().getFields())
        .filter(field -> field.isAnnotationPresent(Inject.class))
        .forEach(field -> {
          try {
            Object targetValue = field.get(target);
            if (targetValue == null) {
              Object found = services.stream()
                      .filter(service -> field.getType().isInstance(service))
                      .findAny()
                      .orElseThrow(() -> new IllegalStateException("No service found to inject into " + field));
              field.set(target, found);
            } else {
              inject(targetValue, services);
            }
          } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
          }
        });
    return target;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  public @interface Inject {}
}
