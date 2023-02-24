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
package org.terracotta.dynamic_config.api.model;

/**
 * @author Mathieu Carbou
 */
public class LockTag {

  // tags when locking before an operation
  public static final String SCALE_OUT_PREFIX = "dynamic-scale:adding:";
  public static final String SCALE_IN_PREFIX = "dynamic-scale:removing:";
  public static final String NODE_ADD_PREFIX = "node:adding:";
  public static final String NODE_DEL_PREFIX = "node:removing:";

  // tags used to prevent further scale operation
  public static final String DENY_SCALE_OUT = "dynamic-scale:adding:deny";
  public static final String DENY_SCALE_IN = "dynamic-scale:removing:deny";

  // tags used to allow retrying a previously failed operation
  public static final String ALLOW_SCALING = "dynamic-scale:allow";

  // owner tags
  public static final String OWNER_PLATFORM = "platform";
}
