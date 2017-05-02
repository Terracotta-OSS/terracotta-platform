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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.CapabilityManagementSupport;

import java.util.Collection;

/**
 * Special management registry that aggregates all created entity management registry on a server to run query and management calls on them.
 * <p>
 * {@link CapabilityManagementSupport} is able to route the call to the right exposed object thanks to the {@link org.terracotta.management.model.context.Context} objects passed.
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface SharedEntityManagementRegistry extends CapabilityManagementSupport {

  Collection<ContextContainer> getContextContainers();

}
