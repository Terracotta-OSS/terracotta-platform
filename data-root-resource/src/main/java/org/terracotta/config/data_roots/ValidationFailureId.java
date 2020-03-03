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
package org.terracotta.config.data_roots;

public enum ValidationFailureId {

  MISMATCHED_DATA_DIR_RESOURCE_NUMBERS(800001),
  PLATFORM_DATA_ROOT_MISSING_IN_ONE(800002),
  DIFFERENT_PLATFORM_DATA_ROOTS(800003),
  MISMATCHED_DATA_DIR_NAMES(800004),
  MULTIPLE_PLATFORM_DATA_ROOTS(800005);

  private final long failureId;

  ValidationFailureId(long failureId) {
    this.failureId = failureId;
  }

  public long getFailureId() {
    return failureId;
  }
}