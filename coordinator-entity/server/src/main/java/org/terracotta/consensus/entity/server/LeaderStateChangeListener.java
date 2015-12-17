package org.terracotta.consensus.entity.server;

import java.util.List;

import org.terracotta.consensus.entity.messages.Nomination;

public interface LeaderStateChangeListener<V> {
  
  void onDelist(Nomination permit, V v);
  
  void onAccept(Nomination permit, List<V> vs);
  
}
