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
package org.terracotta.management.service.registry.provider;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.service.registry.MonitoringResolver;

import java.util.Map;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public abstract class AbstractConsumerManagementProvider<T> extends AbstractManagementProvider<T> implements ConsumerManagementProvider<T> {

  private MonitoringResolver resolver;

  public AbstractConsumerManagementProvider(Class<? extends T> managedType) {
    super(managedType);
  }

  @Override
  public void accept(MonitoringResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public boolean pushServerEntityNotification(T managedObjectSource, String type, Map<String, String> attrs) {
    ExposedObject<T> exposedObject = findExposedObject(managedObjectSource);
    if(exposedObject != null) {
      this.resolver.pushServerEntityNotification(new ContextualNotification(exposedObject.getContext(), type, attrs));
      return true;
    }
    return false;
  }

  protected ClientIdentifier getConnectedClientIdentifier(ClientDescriptor clientDescriptor) {
    return resolver.getConnectedClientIdentifier(clientDescriptor);
  }

  protected long getConsumerId() {
    return resolver.getConsumerId();
  }

  protected void pushServerEntityStatistics(ContextualStatistics... statistics) {
    resolver.pushServerEntityStatistics(statistics);
  }

}
