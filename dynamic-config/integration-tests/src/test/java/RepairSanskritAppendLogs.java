/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import com.terracottatech.persistence.sanskrit.RepairSanskrit;
import com.terracottatech.utilities.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Mathieu Carbou
 */
public class RepairSanskritAppendLogs {
  public static void main(String[] args) throws IOException {
    Files.walk(Paths.get("src/test/resources"))
        .filter(path -> Files.isDirectory(path))
        .filter(path -> path.getFileName().toString().equals("sanskrit"))
        .map(path -> new RepairSanskrit(path, Json.copyObjectMapper(true)))
        .peek(repair -> System.out.println("Repairing append.log in " + repair.getInput()))
        .forEach(RepairSanskrit::repairHashes);
  }
}
