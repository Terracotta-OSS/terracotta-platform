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
package org.terracotta.management.entity.sample.client.management;

import org.terracotta.management.entity.sample.client.HelloWorldEntity;
import org.terracotta.management.registry.action.AbstractActionManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.registry.action.Named;
import org.terracotta.management.registry.action.RequiredContext;

/**
 * @author Mathieu Carbou
 */
@Named("HelloWorldActionProvider")
@RequiredContext({@Named("entityName")})
public class HelloWorldManagementProvider extends AbstractActionManagementProvider<HelloWorldEntity> {
  private final String entityName;

  public HelloWorldManagementProvider(String entityName) {
    super(HelloWorldEntity.class);
    this.entityName = entityName;
  }

  @Override
  protected ExposedObject<HelloWorldEntity> wrap(HelloWorldEntity managedObject) {
    return new ExposedHelloWorldEntity(entityName, managedObject);
  }
}
