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
package org.terracotta.lease.service;

import com.tc.classloader.CommonComponent;
import org.junit.Test;
import org.terracotta.lease.service.closer.ClientConnectionCloser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class LeaseServiceConfigurationTest {
  @Test
  public void getServiceType() {
    ClientConnectionCloser closer = mock(ClientConnectionCloser.class);
    LeaseServiceConfiguration configuration = new LeaseServiceConfiguration(closer);
    assertEquals(LeaseService.class, configuration.getServiceType());
  }

  @Test
  public void getCloser() {
    ClientConnectionCloser closer = mock(ClientConnectionCloser.class);
    LeaseServiceConfiguration configuration = new LeaseServiceConfiguration(closer);
    assertEquals(closer, configuration.getClientConnectionCloser());
  }

  @Test
  public void isCommonComponent() {
    assertNotNull(LeaseServiceConfiguration.class.getAnnotation(CommonComponent.class));
  }
}
