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
package org.terracotta.management.entity.nms.client;

import org.terracotta.management.model.context.Context;

/**
 * @author Mathieu Carbou
 */
public class IllegalManagementCallException extends Exception {
  private static final long serialVersionUID = -5711911050827926344L;

  public IllegalManagementCallException(Context context, String capabilityName, String methodName) {
    super("Cannot find capability " + capabilityName + " on target " + context + " to call method " + methodName);
  }
}
