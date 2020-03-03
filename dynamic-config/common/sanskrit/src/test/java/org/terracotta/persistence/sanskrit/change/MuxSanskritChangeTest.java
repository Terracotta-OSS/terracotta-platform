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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MuxSanskritChangeTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Mock
  private SanskritChange change1;

  @Mock
  private SanskritChange change2;

  @Test
  public void noChanges() {
    MuxSanskritChange change = new MuxSanskritChange(Collections.emptyList());
    change.accept(visitor);
    verifyNoMoreInteractions(visitor);
  }

  @Test
  public void multipleChanges() {
    MuxSanskritChange change = new MuxSanskritChange(Arrays.asList(change1, change2));
    change.accept(visitor);
    verify(change1).accept(visitor);
    verify(change2).accept(visitor);
    verifyNoMoreInteractions(visitor);
  }
}
