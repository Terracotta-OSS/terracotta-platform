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
package org.terracotta.service.reference.holder;

import org.junit.Test;
import org.terracotta.entity.BasicServiceConfiguration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

public class ReferenceHolderServiceProviderTest {

  @Test
  public void testGetProvidedServiceTypes() throws Exception {
    ReferenceHolderServiceProvider provider = new ReferenceHolderServiceProvider();
    assertThat(provider.getProvidedServiceTypes(), hasSize(1));
    assertTrue(provider.getProvidedServiceTypes().contains(ReferenceHolderService.class));
  }

  @Test
  public void testGetService() throws Exception {
    ReferenceHolderServiceProvider provider = new ReferenceHolderServiceProvider();
    BasicServiceConfiguration<ReferenceHolderService> configuration =
        new BasicServiceConfiguration<ReferenceHolderService>(ReferenceHolderService.class);
    ReferenceHolderService service1 = provider.getService(1, configuration);
    assertThat(service1, instanceOf(TransientReferenceHolderService.class));

    ReferenceHolderService service2 = provider.getService(1, configuration);
    assertThat(service2, sameInstance(service1));
  }

  @Test
  public void testClear() throws Exception {
    ReferenceHolderServiceProvider provider = new ReferenceHolderServiceProvider();
    BasicServiceConfiguration<ReferenceHolderService> configuration =
        new BasicServiceConfiguration<ReferenceHolderService>(ReferenceHolderService.class);
    ReferenceHolderService service1 = provider.getService(1, configuration);

    provider.clear();

    ReferenceHolderService service2 = provider.getService(1, configuration);
    assertThat(service2, not(sameInstance(service1)));

  }
}