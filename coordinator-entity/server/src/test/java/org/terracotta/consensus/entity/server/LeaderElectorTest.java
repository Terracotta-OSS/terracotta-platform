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

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.terracotta.consensus.entity.ElectionResponse;
import org.terracotta.consensus.entity.ElectionResult;
import org.terracotta.consensus.entity.LeaderOffer;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.eq;

/**
 * @author Alex Snaps
 */
public class LeaderElectorTest {

  private LeaderElector<String, String> leaderElector;
  private ElectionChangeListener listener;
  
  @Before
  public void setup() {
    leaderElector = new LeaderElector<String, String>(new TestPermitFactory());
    listener = Mockito.mock(ElectionChangeListener.class);
    leaderElector.setListener(listener);
  }
  
  @Test
  public void testLeaderElectionSingleKey() {

    ElectionResponse response1 = leaderElector.enlist("e1", "c1");
    ElectionResponse response2 = leaderElector.enlist("e1", "c2");

    assertThat(((response1).isPending()), is(false));;
    assertThat(((response2).isPending()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e1").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e1").get(0), is("c2"));

    leaderElector.accept("e1", (LeaderOffer) response1);

    ElectionResponse response3 = leaderElector.enlist("e1", "c3");

    assertThat(response3, Is.<ElectionResponse>is(ElectionResult.NOT_ELECTED));

    leaderElector.delist("e1", "c2");
    
    verifyZeroInteractions(listener);
    
    leaderElector.delist("e1", "c1");
    
    verify(listener).onDelist(eq("e1"), eq("c3"));
    
    leaderElector.delist("e1", "c3");
    
    verifyZeroInteractions(listener);
    
    try {
      leaderElector.delist("e1", "c4");
    } catch (Exception expected) {
      assertThat(expected, instanceOf(NullPointerException.class));
    }
    
    ElectionResponse newresponse1 = leaderElector.enlist("e1", "c1");
    assertThat(newresponse1.isPending(), is(false));;
    
    try {
      leaderElector.accept("e1", (LeaderOffer) response1);
    } catch (Exception expected) {
      assertThat(expected, instanceOf(IllegalArgumentException.class));
      assertThat(expected.getMessage(), is("Leader offer not active"));
    }
    
    leaderElector.accept("e1", (LeaderOffer) newresponse1);
    
    ElectionResponse response4 = leaderElector.enlist("e1", "c5");
    
    assertThat(response4, Is.<ElectionResponse>is(ElectionResult.NOT_ELECTED));
  }

  @Test
  public void testLeaderElectionTwoKeys() {

    ElectionResponse permit1 = leaderElector.enlist("e1", "c1");
    ElectionResponse permit2 = leaderElector.enlist("e1", "c2");
    
    ElectionResponse permit1a = leaderElector.enlist("e2", "c1");
    ElectionResponse permit2a = leaderElector.enlist("e2", "c2");

    assertThat(permit1, notNullValue());
    assertThat(((permit1).isPending()), is(false));;
    assertThat(((permit2).isPending()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e1").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e1").get(0), is("c2"));

    assertThat(permit1a, notNullValue());
    assertThat(((permit1a).isPending()), is(false));;
    assertThat(((permit2a).isPending()), is(true));

    assertThat(leaderElector.getAllWaitingOn("e2").size(), is(1));
    assertThat(leaderElector.getAllWaitingOn("e2").get(0), is("c2"));

    
    leaderElector.accept("e1", (LeaderOffer) permit1);
    leaderElector.accept("e2", (LeaderOffer) permit1a);

    ElectionResponse permit3 = leaderElector.enlist("e1", "c3");
    ElectionResponse permit3a = leaderElector.enlist("e2", "c3");

    assertThat(permit3, Is.<ElectionResponse>is(ElectionResult.NOT_ELECTED));
    assertThat(permit3a, Is.<ElectionResponse>is(ElectionResult.NOT_ELECTED));

    leaderElector.delist("e1", "c2");
    leaderElector.delist("e2", "c2");
    
    verifyZeroInteractions(listener);
    
    leaderElector.delist("e1", "c1");
    leaderElector.delist("e2", "c1");
    
    verify(listener, times(2));
  }
  
  @After
  public void tearDown() {
    leaderElector = null;
    listener = null;
  }
  
  private static class TestPermitFactory  implements OfferFactory<String> {
    
    public LeaderOffer createOffer(String t, boolean clean) {
      return new LeaderOffer(clean) {};
    }
    
  }

}