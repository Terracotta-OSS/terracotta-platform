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
package org.terracotta.management.registry;

import org.junit.Test;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.context.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anthony Dahanne
 */
public class DefaultCallQueryTest {

  @Test
  public void execute_handles_unexpected_exceptions() throws Exception {
    final Context context = Context.create("key", "val");
    Parameter[] parameters = new Parameter[]{new Parameter("toto")};
    Collection<Context> contexts = new ArrayList<Context>() {
      private static final long serialVersionUID = 1L;

      {
      add(context);
    }};

    //necessary mock
    ManagementProvider<?> managementProvider = mock(ManagementProvider.class);
    when(managementProvider.supports(context)).thenReturn(true);
    RuntimeException runtimeException = new RuntimeException("Oups, that was not supposed to happen !");
    when(managementProvider.callAction(context, "myMethodName", String.class, parameters)).thenThrow(runtimeException);

    CapabilityManagementSupport capabilityManagementSupport = mock(CapabilityManagementSupport.class);
    Collection<ManagementProvider<?>> managementProviders = new ArrayList<ManagementProvider<?>>();
    managementProviders.add(managementProvider);
    when(capabilityManagementSupport.getManagementProvidersByCapability("myCapabilityName")).thenReturn(managementProviders);

    DefaultCallQuery<String> defaultCallQuery = new DefaultCallQuery<String>(capabilityManagementSupport, "myCapabilityName", "myMethodName", String.class, parameters, contexts);
    ResultSet<ContextualReturn<String>> executeResults = defaultCallQuery.execute();

    ContextualReturn<String> singleResult = executeResults.getSingleResult();
    try {
      singleResult.getValue();
      fail();
    } catch (Exception e) {
      assertThat(e, is(instanceOf(ExecutionException.class)));
      assertThat(e.getMessage(), equalTo("Oups, that was not supposed to happen !"));
    }

  }


}
