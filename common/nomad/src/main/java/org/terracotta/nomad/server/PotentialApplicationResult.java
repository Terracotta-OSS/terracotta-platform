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
package org.terracotta.nomad.server;

public class PotentialApplicationResult<T> {
  private final boolean allowed;
  private final T newConfiguration;
  private final String rejectionReason;

  public static <T> PotentialApplicationResult<T> allow(T newConfiguration) {
    return new PotentialApplicationResult<>(true, newConfiguration, null);
  }

  public static <T> PotentialApplicationResult<T> reject(String reason) {
    return new PotentialApplicationResult<>(false, null, reason);
  }

  private PotentialApplicationResult(boolean allowed, T newConfiguration, String rejectionReason) {
    this.allowed = allowed;
    this.newConfiguration = newConfiguration;
    this.rejectionReason = rejectionReason;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public T getNewConfiguration() {
    return newConfiguration;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }
}
