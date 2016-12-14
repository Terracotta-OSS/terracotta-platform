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
package org.terracotta.voltron.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Mathieu Carbou
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConcurrencyStrategy {

  int key() default MANAGEMENT_KEY;

  /**
   * <p>UNIVERSAL_KEY is a negative key that indicates a request requires no order and can be run concurrently with any
   * other entity operation.</p>
   */
  int UNIVERSAL_KEY = Integer.MIN_VALUE;

  /**
   * <p>MANAGEMENT_KEY is a zero key which is used to manage the entity (anything other than invoke action).</p>
   * <p>Additionally, a message can choose to use this key if it must be run exclusively within the entity.  Be careful with
   * the cases where this is done, however, since the MANAGEMENT_KEY cannot be used for synchronizing data (this is because
   * it would lock-step the system on the synchronization operation).</p>
   * <p>In general, all state within an entity should be "owned" by a specific concurrency key and MANAGEMENT_KEY use
   * minimized.</p>
   */
  int MANAGEMENT_KEY = 0;
}
