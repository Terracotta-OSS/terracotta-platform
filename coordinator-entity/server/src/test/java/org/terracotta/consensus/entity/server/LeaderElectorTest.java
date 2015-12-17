/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.consensus.entity.server;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.terracotta.consensus.entity.messages.Nomination;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.any;

/**
 * @author Alex Snaps
 */
public class LeaderElectorTest {

  private LeaderElector<String, String> leaderElector;
  private DelistListener listener;
  
  @Before
  public void setup() {
    leaderElector = new LeaderElector<String, String>(new TestPermitFactory());
    listener = Mockito.mock(DelistListener.class);
    leaderElector.setListener(listener);
  }
  
  @Test
  public void testLeaderElectionSingleKey() {

    Nomination permit1 = leaderElector.enlist("e1", "c1");
    Nomination permit2 = leaderElector.enlist("e1", "c2");

    assertThat(permit1, notNullValue());
    assertThat(((permit1).awaitsElection()), is(false));;
    assertThat(((permit2).awaitsElection()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e1").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e1").get(0), is("c2"));

    leaderElector.accept("e1", permit1);

    Nomination permit3 = leaderElector.enlist("e1", "c3");

    assertThat(permit3, nullValue());

    leaderElector.delist("e1", "c2");
    
    verifyZeroInteractions(listener);
    
    leaderElector.delist("e1", "c1");
    
    verify(listener).onDelist(any(String.class), any(String.class), any(Nomination.class));
    
    leaderElector.delist("e1", "c3");
    
    verify(listener).onDelist(any(String.class), any(String.class), any(Nomination.class));
    
    try {
      leaderElector.delist("e1", "c4");
    } catch (Exception expected) {
      assertThat(expected, instanceOf(NullPointerException.class));
    }
    
    Nomination newpermit1 = leaderElector.enlist("e1", "c1");
    assertThat(newpermit1, notNullValue());
    assertThat(((newpermit1).awaitsElection()), is(false));;
    
    try {
      leaderElector.accept("e1", permit1);
    } catch (Exception expected) {
      assertThat(expected, instanceOf(IllegalArgumentException.class));
      assertThat(expected.getMessage(), is("Wrong Nomination accepted"));
    }
    
    leaderElector.accept("e1", newpermit1);
    
    Nomination permit4 = leaderElector.enlist("e1", "c5");
    
    assertThat(permit4, nullValue());

  }

  @Test
  public void testLeaderElectionTwoKeys() {

    Nomination permit1 = leaderElector.enlist("e1", "c1");
    Nomination permit2 = leaderElector.enlist("e1", "c2");
    
    Nomination permit1a = leaderElector.enlist("e2", "c1");
    Nomination permit2a = leaderElector.enlist("e2", "c2");

    assertThat(permit1, notNullValue());
    assertThat(((permit1).awaitsElection()), is(false));;
    assertThat(((permit2).awaitsElection()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e1").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e1").get(0), is("c2"));

    assertThat(permit1a, notNullValue());
    assertThat(((permit1a).awaitsElection()), is(false));;
    assertThat(((permit2a).awaitsElection()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e2").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e2").get(0), is("c2"));

    
    leaderElector.accept("e1", permit1);
    leaderElector.accept("e2", permit1a);

    Nomination permit3 = leaderElector.enlist("e1", "c3");
    Nomination permit3a = leaderElector.enlist("e2", "c3");

    assertThat(permit3, nullValue());
    assertThat(permit3a, nullValue());

    leaderElector.delist("e1", "c2");
    leaderElector.delist("e2", "c2");
    
    verifyZeroInteractions(listener);
    
    leaderElector.delist("e1", "c1");
    leaderElector.delist("e2", "c1");
    
    verify(listener, times(2)).onDelist(any(String.class), any(String.class), any(Nomination.class));;
  }
  
  @Test
  public void testLeaderElectionSingleKeyForCarryOverNominationSingleClient() {
    
    Nomination permit1 = leaderElector.enlist("e1", "c1");
    
    assertThat(permit1, notNullValue());
    assertThat(((permit1).awaitsElection()), is(false));
    assertThat(((permit1).isContinuing()), is(false));
    
    leaderElector.delist("e1", "c1");
    
    Nomination permit2 = leaderElector.enlist("e1", "c2");
    
    assertThat(permit2, notNullValue());
    assertThat(((permit2).awaitsElection()), is(false));
    assertThat(((permit2).isContinuing()), is(true));
  }
  
  @After
  public void tearDown() {
    leaderElector = null;
    listener = null;
  }
  
  private static class TestPermitFactory  implements PermitFactory<String, String> {
    
    private static final AtomicLong counter = new AtomicLong();

    public Nomination createPermit(String k, String v, boolean elected) {
      return new Nomination(k, counter.getAndIncrement(), elected);
    }

    public Nomination createPermit(String k) {
      return new Nomination(k);
    }

    
  }

}