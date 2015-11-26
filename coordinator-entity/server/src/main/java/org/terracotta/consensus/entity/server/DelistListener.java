package org.terracotta.consensus.entity.server;

import org.terracotta.consensus.entity.messages.Nomination;


public interface DelistListener<K, V> {
  
  void onDelist(K key, V value, Nomination permit);

}
