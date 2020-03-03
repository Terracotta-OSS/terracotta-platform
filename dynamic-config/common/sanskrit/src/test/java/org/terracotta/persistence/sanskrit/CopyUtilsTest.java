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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.json.Json;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CopyUtilsTest {
  @Mock
  private SanskritVisitor visitor1;
  @Mock
  private SanskritVisitor visitor2;
  private ObjectMapper objectMapper = Json.copyObjectMapper();

  @Test
  public void copyEmpty() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapper);
    SanskritObject sanskritObject = CopyUtils.makeCopy(objectMapper, object);
    sanskritObject.accept(visitor1);

    verifyNoMoreInteractions(visitor1);
  }

  @Test
  public void copyData() {
    SanskritObjectImpl subObject = new SanskritObjectImpl(objectMapper);
    subObject.setString("1", "b");

    SanskritObjectImpl object = new SanskritObjectImpl(objectMapper);
    object.setString("1", "a");
    object.setLong("2", 1L);
    object.setObject("3", subObject);

    SanskritObject sanskritObject = CopyUtils.makeCopy(objectMapper, object);
    sanskritObject.accept(visitor1);
    sanskritObject.getObject("3").accept(visitor2);

    verify(visitor1).setString("1", "a");
    verify(visitor1).setLong("2", 1L);
    verify(visitor2).setString("1", "b");
  }
}
