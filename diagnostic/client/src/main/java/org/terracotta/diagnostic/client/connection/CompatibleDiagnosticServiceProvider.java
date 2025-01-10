/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.model.KitInformation;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * @author Mathieu Carbou
 */
public class CompatibleDiagnosticServiceProvider implements DiagnosticServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CompatibleDiagnosticServiceProvider.class);

  private final DiagnosticServiceProvider delegate;

  public CompatibleDiagnosticServiceProvider(DiagnosticServiceProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public DiagnosticService fetchDiagnosticService(InetSocketAddress address) throws DiagnosticServiceProviderException {
    return checkKitCompatibility(address, delegate.fetchDiagnosticService(address));
  }

  @Override
  public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration connectTimeout) throws DiagnosticServiceProviderException {
    return checkKitCompatibility(address, delegate.fetchDiagnosticService(address, connectTimeout));
  }

  protected DiagnosticService checkKitCompatibility(InetSocketAddress address, DiagnosticService diagnosticService) {
    LOGGER.trace("checkCompatibility()");
    KitInformation kitInformation = diagnosticService.getKitInformation();
    if (!isCompatible(kitInformation)) {
      throw new IllegalArgumentException("Incompatible KIT at address " + address + ":\n" + kitInformation);
    }
    return diagnosticService;
  }

  protected boolean isCompatible(KitInformation kitInformation) {
    return true;
  }

}
