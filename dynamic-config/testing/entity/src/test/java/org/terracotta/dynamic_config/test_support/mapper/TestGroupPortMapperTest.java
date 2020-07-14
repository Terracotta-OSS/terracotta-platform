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
package org.terracotta.dynamic_config.test_support.mapper;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Node;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestGroupPortMapperTest {
  @Test
  public void testPeerGroupPortWhenPropertyNotSet() {
    TestGroupPortMapper testGroupPortMapper = new TestGroupPortMapper();
    Node peerNodeMock = mock(Node.class);
    Node currentNodeMock = mock(Node.class);
    when(peerNodeMock.getNodeGroupPort()).thenReturn(1234);
    @SuppressWarnings("unchecked")
    Map<String, String> map = mock(Map.class);
    when(currentNodeMock.getTcProperties()).thenReturn(map);
    when(currentNodeMock.getTcProperties().containsKey(any())).thenReturn(false);
    assertThat(testGroupPortMapper.getPeerGroupPort(peerNodeMock, currentNodeMock), is(1234));
  }

  @Test
  public void testPeerGroupPortWhenPropertySet() {
    TestGroupPortMapper testGroupPortMapper = new TestGroupPortMapper();
    Node peerNodeMock = mock(Node.class);
    Node currentNodeMock = mock(Node.class);
    @SuppressWarnings("unchecked")
    Map<String, String> map = mock(Map.class);
    when(map.get("test-proxy-group-port")).thenReturn("node-1->2345");
    when(currentNodeMock.getTcProperties()).thenReturn(map);
    when(currentNodeMock.getTcProperties().containsKey(any())).thenReturn(true);
    when(peerNodeMock.getNodeName()).thenReturn("node-1");
    when(peerNodeMock.getNodeGroupPort()).thenReturn(1234);
    assertThat(testGroupPortMapper.getPeerGroupPort(peerNodeMock, currentNodeMock), is(2345));
  }

}