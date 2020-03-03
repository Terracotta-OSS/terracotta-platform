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
package org.terracotta.diagnostic.client.connection;

import java.util.List;

public class DiagnosticServiceProviderException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DiagnosticServiceProviderException(String message) {
    super(message);
  }

  public DiagnosticServiceProviderException(Throwable cause) {
    super(cause);
  }

  public DiagnosticServiceProviderException(String message, Throwable cause) {
    super(message, cause);
  }

  public DiagnosticServiceProviderException(String message, Throwable cause, List<Throwable> suppressed) {
    super(message, cause);
    suppressed.stream().filter(e -> e.equals(cause)).forEach(this::addSuppressed);
  }
}
