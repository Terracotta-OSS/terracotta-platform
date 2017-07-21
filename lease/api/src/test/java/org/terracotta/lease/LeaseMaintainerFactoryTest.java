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
package org.terracotta.lease;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.lease.LeaseEntityConstants.ENTITY_NAME;
import static org.terracotta.lease.LeaseEntityConstants.ENTITY_VERSION;

@RunWith(MockitoJUnitRunner.class)
public class LeaseMaintainerFactoryTest {
  @Spy
  private TestTimeSource timeSource = new TestTimeSource();

  @Mock
  private Connection connection;

  @Mock
  private EntityRef<LeaseAcquirer, Object, Object> entityRef;

  @Mock
  private LeaseAcquirer leaseAcquirer;

  @Before
  public void before() throws Exception {
    TimeSourceProvider.setTimeSource(timeSource);

    when(connection.getEntityRef(LeaseAcquirer.class, ENTITY_VERSION, ENTITY_NAME)).thenReturn(entityRef);
    when(entityRef.fetchEntity(any())).thenReturn(leaseAcquirer);
    when(leaseAcquirer.acquireLease()).thenReturn(6000L);
  }

  @Test
  public void objectsWiredTogetherCorrectly() throws Exception {
    LeaseMaintainer leaseMaintainer = LeaseMaintainerFactory.createLeaseMaintainer(connection);

    verify(timeSource, timeout(1000L).times(1)).sleep(2000L);
    verify(leaseAcquirer, times(1)).acquireLease();
    verify(leaseAcquirer, times(0)).close();

    timeSource.tickMillis(5000L);

    verify(timeSource, timeout(1000L).times(2)).sleep(2000L);
    verify(leaseAcquirer, times(2)).acquireLease();
    verify(leaseAcquirer, times(0)).close();

    leaseMaintainer.close();

    timeSource.tickMillis(5000L);

    verify(timeSource, timeout(1000L).times(2)).sleep(2000L);
    verify(leaseAcquirer, times(2)).acquireLease();
    verify(leaseAcquirer, times(1)).close();
  }
}
