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
package org.terracotta.persistence.sanskrit;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChangeBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

public class SanskritTest {
  private static final String NO_MATCH_HASH = "0000000000000000000000000000000000000000";
  private final SanskritMapper mapper = new JsonSanskritMapper();
  private MemoryFilesystemDirectory filesystemDirectory;

  @Before
  public void before() {
    filesystemDirectory = new MemoryFilesystemDirectory();
  }

  @Test
  public void initHashButNoAppendLog() throws Exception {
    createFileWithContent("hash0", NO_MATCH_HASH);
    loadAndFail();
  }

  @Test
  public void initEmpty() throws Exception {
    loadAndAssertState("key");
    assertNullFiles("append.log", "hash0", "hash1");
  }

  @Test
  public void initSingleRecord() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", logInfo.getHash());

    loadAndAssertState(makeMap("key", "value"));

    assertAppendLog(logInfo.getText());
    assertFile("hash0", logInfo.getHash());
    assertNullFiles("hash1");
  }

  @Test
  public void initOverwrite() throws Exception {
    LogInfo logInfo = LogUtil.createLog(
        makeMap("key", "value1"),
        makeMap("key", "value2")
    );
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash1", logInfo.getHash());

    loadAndAssertState(makeMap("key", "value2"));

    assertAppendLog(logInfo.getText());
    assertFile("hash1", logInfo.getHash());
    assertNullFiles("hash0");
  }

  @Test
  public void initIncorrectRecordHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"));
    String editedText = logInfo.getText().replace("value1", "value2");
    createFileWithContent("append.log", editedText);
    createFileWithContent("hash0", logInfo.getHash());

    loadAndFail();
  }

  @Test
  public void initOneRecordNoFinalHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"));
    createFileWithContent("append.log", logInfo.getText());

    loadAndAssertState("key");

    assertNullFiles("append.log", "hash0", "hash1");
  }

  @Test
  public void initTwoRecordsFinalHashForFirstRecord() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", logInfo.getHash(0));

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertEquals("value1", sanskrit.getString("key"));
    }

    assertAppendLog(logInfo.getText(0));
    assertFile("hash0", logInfo.getHash(0));
    assertNullFiles("hash1");
  }

  @Test
  public void initTwoRecordsNoFinalHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText());

    loadAndFail();
  }

  @Test
  public void initOneRecordIncorrectFinalHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", NO_MATCH_HASH);

    loadAndFail();
  }

  @Test
  public void initTwoRecordsIncorrectFinalHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", NO_MATCH_HASH);

    loadAndFail();
  }

  @Test
  public void partialRecord() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText() + "2018-");
    createFileWithContent("hash0", logInfo.getHash());

    loadAndAssertState(makeMap("key", "value2"));

    assertAppendLog(logInfo.getText());
    assertFile("hash0", logInfo.getHash());
    assertNullFiles("hash1");
  }

  @Test
  public void invalidJson() throws Exception {
    String timestamp = "2018-12-05T17:15:28";
    String json = "json";
    String hash = HashUtils.generateHash(timestamp + LS + json);
    String finalHash = HashUtils.generateHash(hash);
    createFileWithContent("append.log", lines(timestamp, json, hash, ""));
    createFileWithContent("hash0", finalHash);

    loadAndFail();
  }

  @Test
  public void allButLastNewline() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", removeLastNewline(logInfo.getText()));
    createFileWithContent("hash0", logInfo.getHash(0));

    loadAndAssertState(makeMap("key", "value1"));

    assertAppendLog(logInfo.getText(0));
    assertFile("hash0", logInfo.getHash(0));
    assertNullFiles("hash1");
  }

  private String removeLastNewline(String text) {
    String newline = LS;
    if (newline.equals(text.substring(text.length() - newline.length()))) {
      return text.substring(0, text.length() - newline.length());
    }

    return text;
  }

  @Test
  public void partialFinalHash() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", logInfo.getHash(0));
    createFileWithContent("hash1", logInfo.getHash().substring(0, 39));

    loadAndAssertState(makeMap("key", "value1"));

    assertAppendLog(logInfo.getText(0));
    assertFile("hash0", logInfo.getHash(0));
    assertNullFiles("hash1");
  }

  @Test
  public void twoFullHashes() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value1"), makeMap("key", "value2"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", logInfo.getHash(0));
    createFileWithContent("hash1", logInfo.getHash());

    loadAndAssertState(makeMap("key", "value2"));

    assertAppendLog(logInfo.getText());
    assertFile("hash1", logInfo.getHash());
    assertNullFiles("hash0");
  }

  @Test
  public void invalidHashInOtherFile() throws Exception {
    LogInfo logInfo = LogUtil.createLog(makeMap("key", "value"));
    createFileWithContent("append.log", logInfo.getText());
    createFileWithContent("hash0", logInfo.getHash());
    createFileWithContent("hash1", NO_MATCH_HASH);

    loadAndFail();
  }

  @Test
  public void writeString() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.setString("key", "value");
    }

    loadAndAssertState(makeMap("key", "value"));
  }

  @Test
  public void writeMultiple() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.setString("key1", "value1");
      sanskrit.setString("key2", "value2");
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertState(sanskrit, makeMap("key1", "value1", "key2", "value2"));
      sanskrit.setString("key1", "value");
      sanskrit.setString("key3", "value3");
    }

    loadAndAssertState(makeMap("key1", "value", "key2", "value2", "key3", "value3"));
  }

  @Test
  public void writeLong() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.setLong("key1", 3L);
      sanskrit.setLong("key2", -3L);
      sanskrit.setLong("key3", 0);
      sanskrit.setLong("key4", Long.MAX_VALUE);
      sanskrit.setLong("key5", Long.MIN_VALUE);
    }

    loadAndAssertState(makeMap("key1", 3L, "key2", -3L, "key3", 0L, "key4", Long.MAX_VALUE, "key5", Long.MIN_VALUE));
  }

  @Test
  public void writeObject() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObjectImpl object = new SanskritObjectImpl(mapper);
      object.setString("subkey1", "abc");
      object.setLong("subkey2", 1L);

      sanskrit.setObject("key", object);
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObject object = sanskrit.getObject("key");

      assertEquals("abc", object.getString("subkey1"));
      assertEquals(1L, (long) object.getLong("subkey2"));

      SanskritObjectImpl newObject = new SanskritObjectImpl(mapper);
      newObject.setString("subkey1", "def");
      newObject.setLong("subkey3", 2L);
      newObject.setString("key", "overwrite");

      sanskrit.setObject("key", newObject);
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObject object = sanskrit.getObject("key");

      assertEquals("def", object.getString("subkey1"));
      assertNull(object.getLong("subkey2"));
      assertEquals(2L, (long) object.getLong("subkey3"));
      assertEquals("overwrite", object.getString("key"));
    }
  }

  @Test
  public void writeExternal() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObjectImpl object = new SanskritObjectImpl(mapper);
      object.setString("subkey1", "abc");
      object.setLong("subkey2", 1L);

      sanskrit.setObject("key", object);
    }

    TestData.Tomato tomato = new TestData.Tomato(new TestData.TomatoCooking(), "red");
    TestData.Pepper pepper = new TestData.Pepper(new TestData.TomatoCooking(), "spain");

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObject object = sanskrit.getObject("key");

      assertEquals("abc", object.getString("subkey1"));
      assertEquals(1L, (long) object.getLong("subkey2"));

      SanskritObjectImpl newObject = new SanskritObjectImpl(mapper);
      newObject.setString("subkey1", "def");
      newObject.setLong("subkey3", 2L);
      newObject.setString("key", "overwrite");

      newObject.set("tomato", tomato, null);
      newObject.set("pepper", pepper, null);

      sanskrit.setObject("key", newObject);
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObject object = sanskrit.getObject("key");

      assertEquals("def", object.getString("subkey1"));
      assertNull(object.getLong("subkey2"));
      assertEquals(2L, (long) object.getLong("subkey3"));
      assertEquals("overwrite", object.getString("key"));
      assertEquals(tomato, object.get("tomato", TestData.Tomato.class, null));
      assertEquals(pepper, object.get("pepper", TestData.Pepper.class, null));
    }
  }

  @Test
  public void removeKey() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.setString("key", "value1");
      assertEquals("value1", sanskrit.getString("key"));
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      sanskrit.removeKey("key");
      assertNull(sanskrit.getString("key"));
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertNull(sanskrit.getString("key"));
      sanskrit.setString("key", "value2");
      assertEquals("value2", sanskrit.getString("key"));
    }

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertEquals("value2", sanskrit.getString("key"));
    }
  }

  @Test
  public void applyChange() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObjectImpl subSanskritObject = new SanskritObjectImpl(mapper);
      subSanskritObject.setString("a", "b");

      SanskritObjectImpl sanskritObject = new SanskritObjectImpl(mapper);
      sanskritObject.setString("subkey1", "abc");
      sanskritObject.setLong("subkey2", 4L);
      sanskritObject.setObject("subkey3", subSanskritObject);
      sanskritObject.setString("key1", "overwrite");

      SanskritChange change = SanskritChangeBuilder.newChange()
          .setString("key1", "value1")
          .setLong("key2", 2L)
          .setObject("key3", sanskritObject)
          .build();

      sanskrit.applyChange(change);

      assertEquals("value1", sanskrit.getString("key1"));
      assertEquals(2L, (long) sanskrit.getLong("key2"));
      SanskritObject recoveredObject = sanskrit.getObject("key3");
      assertEquals("abc", recoveredObject.getString("subkey1"));
      assertEquals(4L, (long) recoveredObject.getLong("subkey2"));
      SanskritObject subRecoveredObject = recoveredObject.getObject("subkey3");
      assertEquals("b", subRecoveredObject.getString("a"));
      assertEquals("overwrite", recoveredObject.getString("key1"));
    }
  }

  @Test
  public void noSneakyChangesAsWriter() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObjectImpl object = new SanskritObjectImpl(mapper);
      object.setString("A", "B");

      sanskrit.setObject("key", object);

      object.setString("A", "C");

      SanskritObject recoveredObject = sanskrit.getObject("key");
      assertEquals("B", recoveredObject.getString("A"));
    }
  }

  @Test
  public void noSneakyChangesAsReader() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      SanskritObjectImpl subObject = new SanskritObjectImpl(mapper);
      subObject.setString("F", "G");

      SanskritObjectImpl object = new SanskritObjectImpl(mapper);
      object.setString("A", "B");
      object.setObject("D", subObject);

      sanskrit.setObject("key", object);

      SanskritObjectImpl recoveredObject1 = (SanskritObjectImpl) sanskrit.getObject("key");
      recoveredObject1.setString("A", "C");
      SanskritObjectImpl recoveredSubObject1 = (SanskritObjectImpl) recoveredObject1.getObject("D");
      recoveredSubObject1.setString("F", "H");

      SanskritObjectImpl recoveredObject2 = (SanskritObjectImpl) sanskrit.getObject("key");
      assertEquals("B", recoveredObject2.getString("A"));
      SanskritObjectImpl recoveredSubObject2 = (SanskritObjectImpl) recoveredObject2.getObject("D");
      assertEquals("G", recoveredSubObject2.getString("F"));

      assertNull(sanskrit.getObject("unknown"));
      assertNull(recoveredObject1.getObject("unknown"));
      assertNull(recoveredSubObject1.getObject("unknown"));
    }
  }

  @Test(expected = SanskritException.class)
  public void shouldBeUnusableAfterFailure() throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      filesystemDirectory.fail();
      try {
        sanskrit.setString("key", "value");
        fail("Exception expected");
      } catch (SanskritException e) {
        // Expected
      }

      sanskrit.getString("key");
    }
  }

  @Test(expected = SanskritException.class)
  @SuppressWarnings("try")
  public void lockedForSingleUse() throws Exception {
    try (Sanskrit sanskrit1 = Sanskrit.init(filesystemDirectory, mapper)) {
      Sanskrit sanskrit2 = Sanskrit.init(filesystemDirectory, mapper);
    }
  }

  private static String lines(String... lines) {
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line);
      sb.append(LS);
    }
    return sb.toString();
  }

  private static Map<String, Object> makeMap(Object... keysAndValues) {
    Map<String, Object> map = new HashMap<>();

    for (int i = 0; i < keysAndValues.length; i += 2) {
      String key = (String) keysAndValues[i];
      Object value = keysAndValues[i + 1];

      map.put(key, value);
    }

    return map;
  }

  @SuppressWarnings("try")
  private void loadAndFail() throws Exception {
    String appendLog = getFileText("append.log");
    String hash0 = getFileText("hash0");
    String hash1 = getFileText("hash1");

    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      fail("Expected SanskritException");
    } catch (SanskritException e) {
      // expected
    }

    assertFile("append.log", appendLog);
    assertFile("hash0", hash0);
    assertFile("hash1", hash1);
  }

  private void loadAndAssertState(String... missingKeys) throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertState(sanskrit, makeMap(), missingKeys);
    }
  }

  private void loadAndAssertState(Map<String, Object> expected, String... missingKeys) throws Exception {
    try (Sanskrit sanskrit = Sanskrit.init(filesystemDirectory, mapper)) {
      assertState(sanskrit, expected, missingKeys);
    }
  }

  private void assertState(Sanskrit sanskrit, Map<String, Object> expected, String... missingKeys) throws Exception {
    for (Map.Entry<String, Object> entry : expected.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof String) {
        assertEquals(value, sanskrit.getString(key));
      } else if (value instanceof Long) {
        assertEquals(value, sanskrit.getLong(key));
      } else if (value instanceof Map) {
        MapSanskritVisitor childVisitor = new MapSanskritVisitor();
        sanskrit.getObject(key).accept(childVisitor);
        assertEquals(value, childVisitor.getMap());
      } else {
        throw new AssertionError("not tested: " + value);
      }
    }

    for (String missingKey : missingKeys) {
      assertNull(sanskrit.getString(missingKey));
    }
  }

  private void assertAppendLog(String expectedText) throws Exception {
    assertFile("append.log", expectedText);
  }

  private void assertFile(String filename, String expectedText) throws Exception {
    String actualText = getFileText(filename);
    assertEquals(expectedText, actualText);
  }

  private String getFileText(String filename) throws IOException {
    try (FileData fileData = filesystemDirectory.getFileData(filename)) {
      if (fileData == null) {
        return null;
      }

      ByteBuffer bytes = ByteBuffer.allocate((int) fileData.size());
      fileData.read(bytes);
      bytes.flip();
      return StandardCharsets.UTF_8.decode(bytes).toString();
    }
  }

  private void assertNullFiles(String... filenames) throws Exception {
    for (String filename : filenames) {
      assertNull(filesystemDirectory.getFileData(filename));
    }
  }

  private void createFileWithContent(String filename, String text) throws Exception {
    try (FileData fileData = filesystemDirectory.create(filename, false)) {
      ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);

      while (bytes.hasRemaining()) {
        fileData.write(bytes);
      }
    }
  }
}
