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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

//TODO [DYNAMIC-CONFIG]: TDB-4601: Allows to only connect to the online nodes, return only online nodes (fetchDiagnosticServices is throwing at the moment)
public class NomadMessageSender<T> implements AllResultsReceiver<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadMessageSender.class);

  private final Map<InetSocketAddress, NomadEndpoint<T>> serverMap;
  private final String host;
  private final String user;
  private final AsyncCaller asyncCaller;
  private final Map<InetSocketAddress, Long> mutativeMessageCounts = new ConcurrentHashMap<>();
  private final AtomicLong maxVersionNumber = new AtomicLong();
  private volatile Set<InetSocketAddress> servers;

  protected final Set<InetSocketAddress> preparedServers = ConcurrentHashMap.newKeySet();
  protected volatile UUID changeUuid;

  public NomadMessageSender(Collection<NomadEndpoint<T>> servers, String host, String user, AsyncCaller asyncCaller) {
    this.host = host;
    this.user = user;
    this.serverMap = servers.stream().collect(Collectors.toMap(NomadEndpoint::getAddress, s -> s));
    this.servers = serverMap.keySet();
    this.asyncCaller = asyncCaller;
  }

  public void sendDiscovers(DiscoverResultsReceiver<T> results) {
    results.startDiscovery(servers);

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    for (InetSocketAddress serverName : servers) {
      NomadEndpoint<T> server = serverMap.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
          server::discover,
          discovery -> results.discovered(serverName, discovery),
          e -> {
            LOGGER.error("Discover failed: " + e.getMessage(), e);
            results.discoverFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    results.endDiscovery();
  }

  public void sendSecondDiscovers(DiscoverResultsReceiver<T> results) {
    results.startSecondDiscovery();

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    for (InetSocketAddress serverName : servers) {
      NomadEndpoint<T> server = serverMap.get(serverName);
      long mutativeMessageCount = mutativeMessageCounts.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
          server::discover,
          discovery -> {
            long secondMutativeMessageCount = discovery.getMutativeMessageCount();
            if (secondMutativeMessageCount == mutativeMessageCount) {
              results.discoverRepeated(serverName);
            } else {
              String lastMutationHost = discovery.getLastMutationHost();
              String lastMutationUser = discovery.getLastMutationUser();
              results.discoverOtherClient(serverName, lastMutationHost, lastMutationUser);
            }
          },
          e -> {
            LOGGER.error("Discover failed: " + e.getMessage(), e);
            results.discoverFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    // The endSecondDiscovery() call is made outside this method
  }

  public void sendPrepares(PrepareResultsReceiver results, UUID changeUuid, NomadChange change) {
    results.startPrepare(changeUuid);

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    long newVersionNumber = maxVersionNumber.get() + 1;

    for (InetSocketAddress serverName : servers) {
      NomadEndpoint<T> server = serverMap.get(serverName);
      long mutativeMessageCount = mutativeMessageCounts.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
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
              results.prepared(serverName);
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();

              switch (rejectionReason) {
                case UNACCEPTABLE:
                  String rejectionMessage = response.getRejectionMessage();
                  results.prepareChangeUnacceptable(serverName, rejectionMessage);
                  break;
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.prepareOtherClient(serverName, lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + serverName);
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.error("Prepare failed: " + e.getMessage(), e);
            results.prepareFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    results.endPrepare();
  }

  public void sendCommits(CommitResultsReceiver results) {
    results.startCommit();

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    for (InetSocketAddress serverName : preparedServers) {
      NomadEndpoint<T> server = serverMap.get(serverName);
      long mutativeMessageCount = mutativeMessageCounts.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
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
              results.committed(serverName);
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Commit should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.commitOtherClient(serverName, lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + serverName);
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.error("Commit failed: " + e.getMessage(), e);
            results.commitFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    results.endCommit();
  }

  public void sendRollbacks(RollbackResultsReceiver results) {
    results.startRollback();

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    for (InetSocketAddress serverName : preparedServers) {
      NomadEndpoint<T> server = serverMap.get(serverName);
      long mutativeMessageCount = mutativeMessageCounts.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
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
              results.rolledBack(serverName);
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Rollback should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.rollbackOtherClient(serverName, lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + serverName);
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.error("Rollback failed: " + e.getMessage(), e);
            results.rollbackFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    results.endRollback();
  }

  public void sendTakeovers(TakeoverResultsReceiver results) {
    results.startTakeover();

    List<Future<Void>> futures = new ArrayList<>(servers.size());

    for (InetSocketAddress serverName : servers) {
      NomadEndpoint<T> server = serverMap.get(serverName);
      long mutativeMessageCount = mutativeMessageCounts.get(serverName);

      futures.add(asyncCaller.runTimedAsync(
          () -> server.takeover(
              new TakeoverMessage(
                  mutativeMessageCount,
                  host,
                  user
              )
          ),
          response -> {
            if (response.isAccepted()) {
              results.takeover(serverName);
            } else {
              RejectionReason rejectionReason = response.getRejectionReason();
              switch (rejectionReason) {
                case UNACCEPTABLE:
                  throw new AssertionError("Takeover should not return UNACCEPTABLE");
                case DEAD:
                  String lastMutationHost = response.getLastMutationHost();
                  String lastMutationUser = response.getLastMutationUser();
                  results.takeoverOtherClient(serverName, lastMutationHost, lastMutationUser);
                  break;
                case BAD:
                  throw new AssertionError("A server rejected a message as bad: " + serverName);
                default:
                  throw new AssertionError("Unexpected RejectionReason: " + rejectionReason);
              }
            }
          },
          e -> {
            LOGGER.error("Takeover failed: " + e.getMessage(), e);
            results.takeoverFail(serverName, e.getMessage() + "\n" + stackTrace(e));
          }
      ));
    }

    await(futures);

    results.endTakeover();
  }

  private void await(List<Future<Void>> futures) {
    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        throw new AssertionError("Unexpected exception: " + e.getClass() + ": " + e.getMessage(), e);
      }
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    long expectedMutativeMessageCount = discovery.getMutativeMessageCount();
    long highestVersionNumber = discovery.getHighestVersion();

    mutativeMessageCounts.put(server, expectedMutativeMessageCount);
    maxVersionNumber.accumulateAndGet(highestVersionNumber, Long::max);
  }

  private static String stackTrace(Throwable e) {
    StringWriter stack = new StringWriter();
    e.printStackTrace(new PrintWriter(stack));
    return stack.toString();
  }

}
