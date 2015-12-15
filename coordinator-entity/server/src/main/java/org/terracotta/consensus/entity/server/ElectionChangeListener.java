package org.terracotta.consensus.entity.server;

public interface ElectionChangeListener<K, V> {
  
  void onDelist(K key, V value);

}
