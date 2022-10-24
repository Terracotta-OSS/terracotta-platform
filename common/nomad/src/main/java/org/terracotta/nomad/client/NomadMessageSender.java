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
package org.terracotta.nomad.client;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.CommitResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.PrepareResultsReceiver;
import org.terracotta.nomad.client.results.RollbackResultsReceiver;
import org.terracotta.nomad.client.results.TakeoverResultsReceiver;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RejectionReason;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class NomadMessageSender<T> implements AllResultsReceiver<T> {

  private final List<NomadEndpoint<T>> servers;
  private final Clock clock;
  private final String host;
  private final String user;
  private final Map<HostPort, Long> mutativeMessageCounts = new ConcurrentHashMap<>();
  private final AtomicLong maxVersionNumber = new AtomicLong();

  private final List<NomadEndpoint<T>> preparedServers = new CopyOnWriteArrayList<>();
  protected volatile UUID changeUuid;

  public NomadMessageSender(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    this.host = host;
    this.user = user;
    this.servers = servers;
    this.clock = clock;
  }

  public void sendDiscovers(DiscoverResultsReceiver<T> results) {
    results.startDiscovery(servers.stream().map(NomadEndpoint::getHostPort).collect(toList()));
    for (NomadEndpoint<T> server : servers) {
      runSync(
          server::discover,
          discovery -> results.discovered(server.getHostPort(), discovery),
          unwrap(e -> results.discoverFail(server.getHostPort(), e))
      );
    }

    results.endDiscovery();
  }

  public void sendSecondDiscovers(DiscoverResultsReceiver<T> results) {
    results.startSecondDiscovery();

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getHostPort());
      runSync(
          server::discover,
          discovery -> {
            long secondMutativeMessageCount = discovery.getMutativeMessageCount();
            if (secondMutativeMessageCount == mutativeMessageCount) {
              results.discoverRepeated(server.getHostPort());
            } else {
              String lastMutationHost = discovery.getLastMutationHost();
              String lastMutationUser = discovery.getLastMutationUser();
              results.discoverOtherClient(server.getHostPort(), lastMutationHost, lastMutationUser);
            }
          },
          unwrap(e -> results.discoverFail(server.getHostPort(), e))
      );
    }

    // The endSecondDiscovery() call is made outside this method
  }

  public void sendPrepares(PrepareResultsReceiver results, UUID changeUuid, NomadChange change) {
    results.startPrepare(changeUuid);

    long newVersionNumber = maxVersionNumber.get() + 1;
    Instant now = clock.instant();

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getHostPort());
      runSync(
          () -> server.prepare(
              new PrepareMessage(
                  mutativeMessageCount,
                  host,
                  user,
                  now,
                  changeUuid,
                  newVersionNumber,
                  change
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.prepared(server.getHostPort());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();

              switch (rejectionReason) {
                case UNACCEPTABLE:
                  String rejectionMessage = response.getRejectionMessage();
                  results.prepareChangeUnacceptable(server.getHostPort(), rejectionMessage);
                  break;
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.prepareOtherClient(server.getHostPort(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getHostPort());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          unwrap(e -> results.prepareFail(server.getHostPort(), e))
      );
    }

    results.endPrepare();
  }

  public void sendCommits(CommitResultsReceiver results) {
    results.startCommit();

    Instant now = clock.instant();

    for (NomadEndpoint<T> server : preparedServers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getHostPort());
      runSync(
          () -> {
            return server.commit(
                new CommitMessage(
                    mutativeMessageCount + 1,
                    host,
                    user,
                    now,
                    changeUuid
                )
            );
          },
          response -> {
            if (response.isAccepted()) {
              results.committed(server.getHostPort());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Commit should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.commitOtherClient(server.getHostPort(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getHostPort());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          unwrap(e -> results.commitFail(server.getHostPort(), e))
      );
    }

    results.endCommit();
  }

  public void sendRollbacks(RollbackResultsReceiver results) {
    results.startRollback();

    Instant now = clock.instant();

    for (NomadEndpoint<T> server : preparedServers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getHostPort());
      runSync(
          () -> server.rollback(
              new RollbackMessage(
                  mutativeMessageCount + 1,
                  host,
                  user,
                  now,
                  changeUuid
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.rolledBack(server.getHostPort());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Rollback should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.rollbackOtherClient(server.getHostPort(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getHostPort());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          unwrap(e -> results.rollbackFail(server.getHostPort(), e))
      );
    }

    results.endRollback();
  }

  public void sendTakeovers(TakeoverResultsReceiver results) {
    results.startTakeover();

    Instant now = clock.instant();

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getHostPort());
      runSync(
          () -> server.takeover(
              new TakeoverMessage(
                  mutativeMessageCount,
                  host,
                  user,
                  now
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.takeover(server.getHostPort());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Takeover should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.takeoverOtherClient(server.getHostPort(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getHostPort());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          unwrap(e -> results.takeoverFail(server.getHostPort(), e))
      );
    }

    results.endTakeover();
  }

  @Override
  public void discovered(HostPort server, DiscoverResponse<T> discovery) {
    long expectedMutativeMessageCount = discovery.getMutativeMessageCount();
    long highestVersionNumber = discovery.getHighestVersion();

    mutativeMessageCounts.put(server, expectedMutativeMessageCount);
    maxVersionNumber.accumulateAndGet(highestVersionNumber, Long::max);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final void registerPreparedServer(HostPort address) {
    preparedServers.add(servers.stream().filter(s -> s.getHostPort().equals(address)).findAny().get());
  }

  private <T> void runSync(Callable<T> callable, Consumer<T> onSuccess, Consumer<Throwable> onError) {
    try {
      T result = callable.call();
      if (result == null) {
        throw new AssertionError("Response expected. Bug or wrong mocking ?");
      }
      onSuccess.accept(result);
    } catch (Exception e) {
      onError.accept(e);
    }
  }

  private static Consumer<Throwable> unwrap(Consumer<Throwable> c) {
    return t -> c.accept(t instanceof NomadException && t.getCause() != null && t.getCause() != t ? t.getCause() : t);
  }
}
