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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CopyUtilsTest {
  @Mock
  private SanskritVisitor visitor1;
  @Mock
  private SanskritVisitor visitor2;
  private final SanskritMapper mapper = new JsonSanskritMapper();

  @Test
  public void copyEmpty() throws SanskritException {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    SanskritObjectImpl sanskritObject = new SanskritObjectImpl(mapper);
    object.accept(sanskritObject);
    sanskritObject.accept(visitor1);

    verifyNoMoreInteractions(visitor1);
  }

  @Test
  public void copyData() throws SanskritException {
    SanskritObjectImpl subObject = new SanskritObjectImpl(mapper);
    subObject.setString("1", "b");

    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setString("1", "a");
    object.setLong("2", 1L);
    object.setObject("3", subObject);

    SanskritObjectImpl sanskritObject = new SanskritObjectImpl(mapper);
    object.accept(sanskritObject);
    sanskritObject.accept(visitor1);
    sanskritObject.getObject("3").accept(visitor2);

    verify(visitor1).set("1", "a", "1");
    verify(visitor1).set("2", 1L, "1");
    verify(visitor2).set("1", "b", "1");
  }
}
