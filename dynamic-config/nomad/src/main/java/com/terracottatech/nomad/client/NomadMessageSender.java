/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.client.results.CommitResultsReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.client.results.PrepareResultsReceiver;
import com.terracottatech.nomad.client.results.RollbackResultsReceiver;
import com.terracottatech.nomad.client.results.TakeoverResultsReceiver;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadMessageSender.class);

  private final List<NomadEndpoint<T>> servers;
  private final String host;
  private final String user;
  private final Map<InetSocketAddress, Long> mutativeMessageCounts = new ConcurrentHashMap<>();
  private final AtomicLong maxVersionNumber = new AtomicLong();

  private final List<NomadEndpoint<T>> preparedServers = new CopyOnWriteArrayList<>();
  protected volatile UUID changeUuid;

  public NomadMessageSender(List<NomadEndpoint<T>> servers, String host, String user) {
    this.host = host;
    this.user = user;
    this.servers = servers;
  }

  public void sendDiscovers(DiscoverResultsReceiver<T> results) {
    results.startDiscovery(servers.stream().map(NomadEndpoint::getAddress).collect(toList()));
    for (NomadEndpoint<T> server : servers) {
      runSync(
          server::discover,
          discovery -> results.discovered(server.getAddress(), discovery),
          e -> {
            LOGGER.error("Discover failed: " + e.getMessage(), e);
            results.discoverFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    results.endDiscovery();
  }

  public void sendSecondDiscovers(DiscoverResultsReceiver<T> results) {
    results.startSecondDiscovery();

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getAddress());
      runSync(
          server::discover,
          discovery -> {
            long secondMutativeMessageCount = discovery.getMutativeMessageCount();
            if (secondMutativeMessageCount == mutativeMessageCount) {
              results.discoverRepeated(server.getAddress());
            } else {
              String lastMutationHost = discovery.getLastMutationHost();
              String lastMutationUser = discovery.getLastMutationUser();
              results.discoverOtherClient(server.getAddress(), lastMutationHost, lastMutationUser);
            }
          },
          e -> {
            LOGGER.debug("Discover failed: " + e.getMessage(), e);
            results.discoverFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    // The endSecondDiscovery() call is made outside this method
  }

  public void sendPrepares(PrepareResultsReceiver results, UUID changeUuid, NomadChange change) {
    results.startPrepare(changeUuid);

    long newVersionNumber = maxVersionNumber.get() + 1;

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getAddress());
      runSync(
          () -> server.prepare(
              new PrepareMessage(
                  mutativeMessageCount,
                  host,
                  user,
                  changeUuid,
                  newVersionNumber,
                  change
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.prepared(server.getAddress());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();

              switch (rejectionReason) {
                case UNACCEPTABLE:
                  String rejectionMessage = response.getRejectionMessage();
                  results.prepareChangeUnacceptable(server.getAddress(), rejectionMessage);
                  break;
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.prepareOtherClient(server.getAddress(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getAddress());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.debug("Prepare failed: " + e.getMessage(), e);
            results.prepareFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    results.endPrepare();
  }

  public void sendCommits(CommitResultsReceiver results) {
    results.startCommit();

    for (NomadEndpoint<T> server : preparedServers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getAddress());
      runSync(
          () -> server.commit(
              new CommitMessage(
                  mutativeMessageCount + 1,
                  host,
                  user,
                  changeUuid
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.committed(server.getAddress());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Commit should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.commitOtherClient(server.getAddress(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getAddress());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.debug("Commit failed: " + e.getMessage(), e);
            results.commitFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    results.endCommit();
  }

  public void sendRollbacks(RollbackResultsReceiver results) {
    results.startRollback();

    for (NomadEndpoint<T> server : preparedServers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getAddress());
      runSync(
          () -> server.rollback(
              new RollbackMessage(
                  mutativeMessageCount + 1,
                  host,
                  user,
                  changeUuid
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.rolledBack(server.getAddress());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Rollback should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.rollbackOtherClient(server.getAddress(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getAddress());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.debug("Rollback failed: " + e.getMessage(), e);
            results.rollbackFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    results.endRollback();
  }

  public void sendTakeovers(TakeoverResultsReceiver results) {
    results.startTakeover();

    for (NomadEndpoint<T> server : servers) {
      long mutativeMessageCount = mutativeMessageCounts.get(server.getAddress());
      runSync(
          () -> server.takeover(
              new TakeoverMessage(
                  mutativeMessageCount,
                  host,
                  user
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.takeover(server.getAddress());
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Takeover should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.takeoverOtherClient(server.getAddress(), lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + server.getAddress());
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.debug("Takeover failed: " + e.getMessage(), e);
            results.takeoverFail(server.getAddress(), e.getMessage() + "\n" + stackTrace(e));
          }
      );
    }

    results.endTakeover();
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    long expectedMutativeMessageCount = discovery.getMutativeMessageCount();
    long highestVersionNumber = discovery.getHighestVersion();

    mutativeMessageCounts.put(server, expectedMutativeMessageCount);
    maxVersionNumber.accumulateAndGet(highestVersionNumber, Long::max);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final void registerPreparedServer(InetSocketAddress address) {
    preparedServers.add(servers.stream().filter(s -> s.getAddress().equals(address)).findFirst().get());
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

  private static String stackTrace(Throwable e) {
    StringWriter stack = new StringWriter();
    e.printStackTrace(new PrintWriter(stack));
    return stack.toString();
  }

}
