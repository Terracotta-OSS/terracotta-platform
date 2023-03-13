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
package org.terracotta.voter;

import com.tc.util.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.Diagnostics;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.voter.ClientVoterManager.INVALID_VOTER_RESPONSE;
import static org.terracotta.voter.ClientVoterManagerImpl.REQUEST_TIMEOUT;
import static org.terracotta.voter.ClientVoterManagerImpl.MBEAN_NAME;

public class ClientVoterManagerImplTest {

  private ClientVoterManagerImpl manager = new ClientVoterManagerImpl("foo:6543");
  private Diagnostics diagnostics = mock(Diagnostics.class);

  @Before
  public void setUp() throws Exception {
    manager.diagnostics = diagnostics;
  }

  @Test
  public void testRegister() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn("123");
    assertThat(manager.register("foo"), is(Boolean.TRUE));
  }

  @Test(expected = RuntimeException.class)
  public void testRegisterFailure() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn(Long.toString(INVALID_VOTER_RESPONSE));
    assertThat(manager.register("foo"), is(Boolean.FALSE));
  }

  @Test(expected = TimeoutException.class)
  public void testRegisterTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", "foo")).thenReturn(REQUEST_TIMEOUT);
    manager.register("foo");
  }

  @Test
  public void testHeartbeat() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn("123");
    assertThat(manager.heartbeat("foo"), is(123L));
  }

  @Test(expected = RuntimeException.class)
  public void testHeartbeatInvalidVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn(Long.toString(INVALID_VOTER_RESPONSE));
    assertThat(manager.heartbeat("foo"), is(INVALID_VOTER_RESPONSE));
  }

  @Test(expected = TimeoutException.class)
  public void testHeartbeatTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo")).thenReturn(REQUEST_TIMEOUT);
    manager.heartbeat("foo");
  }

  @Test
  public void testVote() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn("123");
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo:123")).thenReturn("123");
    manager.heartbeat("foo:123");
    assertThat(manager.vote("foo"), is(123L));
  }

  @Test(expected = RuntimeException.class)
  public void testVoteInvalidVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn(Long.toString(INVALID_VOTER_RESPONSE));
    manager.vote("foo");
  }

  @Test(expected = TimeoutException.class)
  public void testVoteTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "vote", "foo:123")).thenReturn(REQUEST_TIMEOUT);
    when(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", "foo:123")).thenReturn("123");
    manager.heartbeat("foo:123");
    manager.vote("foo");
  }

  @Test
  public void testOverrideVote() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "overrideVote", "foo")).thenReturn("true");
    assertThat(manager.overrideVote("foo"), is(true));
  }

  @Test
  public void testOverrideVoteFailure() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "overrideVote", "foo")).thenReturn("false");
    manager.overrideVote("foo");
  }

  @Test
  public void testOverrideVoteTimeout() {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "overrideVote", "foo")).thenReturn(REQUEST_TIMEOUT);
    assertThat(manager.overrideVote("foo"), is(false));
  }

  @Test
  public void testDeregisterVoter() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", "foo")).thenReturn("true");
    assertThat(manager.deregisterVoter("foo"), is(true));
  }

  @Test(expected = TimeoutException.class)
  public void testDeregisterVoterTimeout() throws TimeoutException {
    when(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", "foo")).thenReturn(REQUEST_TIMEOUT);
    manager.deregisterVoter("foo");
  }
}