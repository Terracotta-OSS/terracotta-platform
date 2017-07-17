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
package org.terracotta.management.entity.nms.agent.client.diag;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.action.AbstractActionManagementProvider;
import org.terracotta.management.registry.action.Exposed;
import org.terracotta.management.registry.ExposedObject;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Anthony Dahanne
 */
@Named("DiagnosticCalls")
@RequiredContext(value = {})
public class DiagnosticProvider extends AbstractActionManagementProvider<DiagnosticUtility> {


  public DiagnosticProvider(Class<? extends DiagnosticUtility> managedType) {
    super(managedType);
  }

  @Override
  protected ExposedObject<DiagnosticUtility> wrap(DiagnosticUtility managedObject) {
    return new ExposedDiagnosticUtility(managedObject);
  }

  public static class ExposedDiagnosticUtility implements ExposedObject<DiagnosticUtility> {

    private final DiagnosticUtility diagnosticUtility;

    public ExposedDiagnosticUtility(DiagnosticUtility diagnosticUtility) {
      this.diagnosticUtility = diagnosticUtility;
    }

    @Exposed
    public String getThreadDump() {
      return diagnosticUtility.getThreadDump();
    }

    @Override
    public DiagnosticUtility getTarget() {
      return diagnosticUtility;
    }

    @Override
    public ClassLoader getClassLoader() {
      return diagnosticUtility.getClass().getClassLoader();
    }

    @Override
    public Context getContext() {
      return Context.empty();
    }

    @Override
    public Collection<? extends Descriptor> getDescriptors() {
      return Collections.emptyList();
    }
  }


}
