/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.runnel.docs;

import org.junit.Test;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
@SuppressWarnings("rawtypes")
public class GettingStarted {

  @Test
  public void createSimpleStructure() throws Exception {
    // tag::createSimpleStructure[]
    Struct struct = StructBuilder.newStructBuilder() // <1>
        .string("firstName", 1) // <2>
        .string("lastName", 2)
        .int64("age", 3)
        .build(); // <3>
    // end::createSimpleStructure[]
  }

  @Test
  public void encodeSimpleStructure() throws Exception {
    // tag::encodeSimpleStructure[]
    Struct struct = StructBuilder.newStructBuilder() // <1>
        .string("firstName", 1)
        .string("lastName", 2)
        .int64("age", 3)
        .build();

    ByteBuffer buffer = struct.encoder() // <2>
        .string("firstName", "john") // <3>
        .string("lastName", "doe")
        .int64("age", 30)
        .encode(); // <4>
    // end::encodeSimpleStructure[]
  }

  @Test
  public void decodeSimpleStructure() throws Exception {
    // tag::decodeSimpleStructure[]
    Struct struct = StructBuilder.newStructBuilder() // <1>
        .string("firstName", 1)
        .string("lastName", 2)
        .int64("age", 3)
        .build();

    ByteBuffer buffer = struct.encoder() // <2>
        .string("firstName", "john")
        .string("lastName", "doe")
        .int64("age", 30)
        .encode();

    buffer.rewind(); // <3>

    StructDecoder decoder = struct.decoder(buffer); // <4>

    String firstName = decoder.string("firstName"); // <5>
    String lastName = decoder.string("lastName");
    Long age = decoder.int64("age");
    // end::decodeSimpleStructure[]
  }

}
