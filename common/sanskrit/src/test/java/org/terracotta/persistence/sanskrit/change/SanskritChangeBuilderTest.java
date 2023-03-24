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
package org.terracotta.persistence.sanskrit.change;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.persistence.sanskrit.JsonSanskritMapper;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.SanskritObjectImpl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SanskritChangeBuilderTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Test
  public void buildChange() throws SanskritException {
    SanskritObject object = new SanskritObjectImpl(new JsonSanskritMapper());

    SanskritChange change = SanskritChangeBuilder.newChange()
        .setString("1", "a")
        .setLong("2", 1L)
        .setObject("3", object)
        .removeKey("4")
        .build();

    change.accept(visitor);

    InOrder inOrder = inOrder(visitor);

    inOrder.verify(visitor).setString("1", "a");
    inOrder.verify(visitor).setLong("2", 1L);
    inOrder.verify(visitor).setObject("3", object);
    inOrder.verify(visitor).removeKey("4");

    verifyNoMoreInteractions(visitor);
  }

  @Test
  public void duplicateKey() throws SanskritException {
    SanskritChange change = SanskritChangeBuilder.newChange()
        .setString("1", "a")
        .setString("1", "b")
        .build();

    change.accept(visitor);

    InOrder inOrder = inOrder(visitor);

    inOrder.verify(visitor).setString("1", "a");
    inOrder.verify(visitor).setString("1", "b");

    verifyNoMoreInteractions(visitor);
  }
}
