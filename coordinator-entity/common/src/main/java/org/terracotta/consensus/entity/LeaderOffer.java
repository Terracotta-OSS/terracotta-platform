/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus.entity;

/**
 *
 * @author cdennis
 */
public abstract class LeaderOffer implements ElectionResponse {

  private final boolean clean;

  public LeaderOffer(boolean clean) {
    this.clean = clean;
  }

  public final boolean isPending() {
    return false;
  }

  public final boolean clean() {
    return clean;
  }
}
