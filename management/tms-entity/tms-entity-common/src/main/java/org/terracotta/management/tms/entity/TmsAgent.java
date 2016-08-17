package org.terracotta.management.tms.entity;

import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.message.Message;
import org.terracotta.voltron.proxy.Async;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
public interface TmsAgent {

  @Async(Async.Ack.NONE)
  Future<Cluster> readTopology();

  @Async(Async.Ack.NONE)
  Future<List<Message>> readMessages();

}
