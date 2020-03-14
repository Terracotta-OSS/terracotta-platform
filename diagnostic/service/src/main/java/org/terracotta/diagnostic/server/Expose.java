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
package org.terracotta.diagnostic.server;

import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to determine if a diagnostic service will be exposed or not through JMX.
 * <p>
 * By default, diagnostic services are private and not expose so this annotation is necessary
 * to get a service exposed.
 * <p>
 * Note: whether a diagnostic service is exposed or not through JMX, we will still have access
 * to it through the diagnostic service proxy mechanism.
 * <p>
 * This annotation only determines of the diagnostic service will also be made available in a
 * JMX console
 * <p>
 * You can also expose a service that is not annotated with {@link DiagnosticServicesRegistration}
 *
 * @author Mathieu Carbou
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Expose {
  /**
   * Mbean name
   */
  String value();
}
