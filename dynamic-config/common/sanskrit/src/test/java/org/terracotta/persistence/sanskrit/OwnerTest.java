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
package org.terracotta.persistence.sanskrit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.terracotta.persistence.sanskrit.Owner.own;

@RunWith(MockitoJUnitRunner.class)
public class OwnerTest {
  @Mock
  private Closeable contained;

  @Test
  @SuppressWarnings("try")
  public void closesResourceIfNotReleased() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
    }

    verify(contained).close();
  }

  @Test
  public void doesNotCloseResourceIfReleased() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.release();
    }

    verifyNoMoreInteractions(contained);
  }

  @Test
  public void borrowHasNoImpactOnClose() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.borrow();
    }

    verify(contained).close();
  }

  @Test
  public void borrowHasNoImpactOnRelease() throws Exception {
    try (Owner<Closeable, IOException> owner = own(contained, IOException.class)) {
      owner.borrow();
      owner.release();
    }

    verifyNoMoreInteractions(contained);
  }

  @Test
  public void copeWithNullResource() throws Exception {
    try (Owner<Closeable, IOException> owner = own(null, IOException.class)) {
      assertNull(owner.borrow());
    }
  }

  @Test
  public void copeWithNullResourceRelease() throws Exception {
    try (Owner<Closeable, IOException> owner = own(null, IOException.class)) {
      assertNull(owner.borrow());
      assertNull(owner.release());
    }
  }
}
