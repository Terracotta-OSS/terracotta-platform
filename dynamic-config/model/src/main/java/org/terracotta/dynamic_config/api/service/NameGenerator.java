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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class NameGenerator {

  private static class TrieNode {
    private final Map<Character, TrieNode> children = new HashMap<>();
    private boolean isLeaf;
  }

  private static final String[] dictionaryFiles = {"animals.txt", "space.txt", "countries.txt", "sports.txt"};
  private static final Map<String, Integer> category = new HashMap<>();
  private static int stripeFallbackCount = 0;
  private static int nodeFallbackCount = 0;
  private static final TrieNode root = new TrieNode();

  static {
    createDictionary();
  }

  /**
   * Assign friendly names on all the stripes and nodes on a cluster
   */
  public static void assignFriendlyNames(Cluster cluster) {
    cluster.getStripes().forEach(stripe -> assignFriendlyNames(cluster, stripe));
  }

  /**
   * Assign friendly names to a whole stripe in a cluster, plus its nodes
   */
  public static void assignFriendlyNames(Cluster cluster, Stripe stripe) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    assignFriendlyStripeName(cluster, stripe, random);
    stripe.getNodes().forEach(node -> assignFriendlyNodeName(cluster, node, random));
  }

  /**
   * Assign a friendly name to a node in this cluster
   */
  public static void assignFriendlyNodeName(Cluster cluster, Node node) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    assignFriendlyNodeName(cluster, node, random);
  }

  /**
   * Assign some friendly stripe names in a cluster, without touching the node names,
   * and uses a given random to control the generation
   */
  public static void assignFriendlyStripeNames(Cluster cluster, Random random) {
    cluster.getStripes().forEach(stripe -> assignFriendlyStripeName(cluster, stripe, random));
  }

  private static void assignFriendlyNodeName(Cluster cluster, Node node, Random random) {
    if (nameCanBeSet(node)) {
      Stripe stripe = cluster.getStripeByNode(node.getUID()).orElseThrow(IllegalArgumentException::new);
      List<String> used = stripe.getNodes().stream().map(Node::getName).collect(toList());
      List<String> dict = readLines("dict/greek.txt");
      node.setName(pickRandomNodeName(dict, used, random, stripe.getName() + "-"));
    }
  }

  private static void assignFriendlyStripeName(Cluster cluster, Stripe stripe, Random random) {
    if (nameCanBeSet(stripe)) {
      List<String> used = cluster.getStripes().stream().map(Stripe::getName).collect(toList());
      String allreadyGeneratedStripeName = null;
      for (String stripeName : used) {
        if (isPresentInDictionary(stripeName)) {
          allreadyGeneratedStripeName = stripeName;
          break;
        }
      }
      String fileName;
      if (allreadyGeneratedStripeName == null) {
        int ind = random.nextInt(dictionaryFiles.length);
        fileName = dictionaryFiles[ind];
      } else {
        fileName = dictionaryFiles[category.get(allreadyGeneratedStripeName)];
      }
      List<String> dict = readLines("dict/" + fileName);
      stripe.setName(pickRandomStripeName(dict, used, random));
    }
  }

  private static void createDictionary() {
    for (int i = 0; i < dictionaryFiles.length; ++i) {
      String fileName = dictionaryFiles[i];
      int ind = i;
      List<String> allLines = readLines("dict/" + fileName);
      allLines.forEach(line -> {
        insertInDictionary(line);
        category.put(line, ind);
      });
    }
  }

  private static void insertInDictionary(String s) {
    TrieNode tmp = root;
    for (int i = 0; i < s.length(); ++i) {
      TrieNode next = tmp.children.get(s.charAt(i));
      if (next == null) {
        next = new TrieNode();
        tmp.children.put(s.charAt(i), next);
      }
      tmp = next;
    }
    tmp.isLeaf = true;
  }

  private static boolean isPresentInDictionary(String s) {
    if (s == null) {
      return false;
    }
    TrieNode tmp = root;
    for (int i = 0; i < s.length(); ++i) {
      TrieNode next = tmp.children.get(s.charAt(i));
      if (next == null) {
        return false;
      }
      tmp = next;
    }
    return tmp.isLeaf;
  }

  private static List<String> readLines(String resource) {
    List<String> list = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(requireNonNull(Setting.class.getClassLoader().getResourceAsStream(resource)), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        list.add(line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return list;
  }

  private static String pickRandomNodeName(List<String> dict, List<String> used, Random random, String prefix) {
    dict = new ArrayList<>(dict);
    dict.removeAll(used);

    if (dict.isEmpty()) {
      String name;
      do {
        nodeFallbackCount++;
        name = prefix + nodeFallbackCount;
      } while (used.contains(name));
      return name;
    }

    if (dict.size() == 1) {
      return prefix + dict.get(0);
    }

    return prefix + dict.get(random.nextInt(dict.size()));
  }

  private static String pickRandomStripeName(List<String> dict, List<String> used, Random random) {
    dict = new ArrayList<>(dict);
    dict.removeAll(used);

    if (dict.isEmpty()) {
      String name;
      do {
        stripeFallbackCount++;
        name = "stripe-" + stripeFallbackCount;
      } while (used.contains(name));
      return name;
    }

    if (dict.size() == 1) {
      return dict.get(0);
    }

    return dict.get(random.nextInt(dict.size()));
  }

  private static boolean nameCanBeSet(Node node) {
    String name = node.getName();
    return name == null || name.startsWith("node-") && UID.isUID(name.substring(5));
  }

  private static boolean nameCanBeSet(Stripe stripe) {
    String name = stripe.getName();
    return name == null || name.startsWith("stripe-") && UID.isUID(name.substring(7));
  }
}
