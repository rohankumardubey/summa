package org.apache.lucene.util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TestChunkedLongArray extends TestCase {

  public void testGrowth() throws Exception {
    ChunkedLongArray chunked = new ChunkedLongArray();

    chunked.add(1);
    chunked.add(3);
    assertEquals(new long[]{1, 3}, chunked);
  }

  public void testSet() throws Exception {
    ChunkedLongArray chunked = new ChunkedLongArray();
    chunked.set(1, 2);
    chunked.set(1000, 3);
    chunked.set((int) Math.pow(2, ChunkedLongArray.DEFAULT_CHUNK_BITS) - 1, 3);
    chunked.set((int) Math.pow(2, ChunkedLongArray.DEFAULT_CHUNK_BITS), 3);
    chunked.set((int) Math.pow(2, ChunkedLongArray.DEFAULT_CHUNK_BITS) + 1, 4);
  }

  public void testMicroSort() {
    final long[] EXPECTED = new long[]{1, 2};
    ChunkedLongArray chunked = new ChunkedLongArray();

    chunked.add(2);
    chunked.add(1);

    chunked.sort();
    assertEquals(EXPECTED, chunked);
  }

  public void testSortMonkey() {
    final int CHUNK_BITS = 4;
    final int CHUNK_SIZE = (int) Math.pow(2, CHUNK_BITS);
    final int ADDS = CHUNK_SIZE*4+1;

    ChunkedLongArray chunked = getChunked(CHUNK_BITS, ADDS, new Random(87));
    long[] expected = getLongArray(ADDS, new Random(87));
    // Sanity check for standard addition
    assertEquals(expected, chunked);

    chunked.sort();
    Arrays.sort(expected);
    assertEquals(expected, chunked);
  }

  private ChunkedLongArray getChunked(int cBits, int adds, Random random) {
    ChunkedLongArray chunked = new ChunkedLongArray(cBits);
    for (int i = 0 ; i < adds ; i++) {
      chunked.add(random.nextLong());
    }
    return chunked;
  }

  private long[] getLongArray(int adds, Random random) {
    long[] la = new long[adds];
    for (int i = 0 ; i < adds ; i++) {
      la[i] = random.nextLong();
    }
    return la;
  }

  public void testCopy() {
    ChunkedLongArray chunkedSrc = new ChunkedLongArray();
    ChunkedLongArray chunkedDest = new ChunkedLongArray();
    for (int i = 0 ; i < 10 ; i++) {
      chunkedSrc.add(9-i); // 9, 8, 7...
      chunkedDest.add(i);  // 0, 1, 2...
    }
    chunkedDest.set(chunkedSrc, 2, 1, 3); // 0, 7, 6, 5, 4, 5, 6, 7, 8, 9
    assertEquals(new long[]{0, 7, 6, 5, 4, 5, 6, 7, 8, 9}, chunkedDest);
  }

  public void testCopyMonkey() {
    final int CHUNK_BITS = 4;
    final int CHUNK_SIZE = (int) Math.pow(2, CHUNK_BITS);
    final int ENTRIES = CHUNK_SIZE*4+1;

    final int COPIES = 20;

    ChunkedLongArray chunked1 = getChunked(CHUNK_BITS, ENTRIES, new Random(87));
    ChunkedLongArray chunked2 = getChunked(CHUNK_BITS, ENTRIES, new Random(88));

    long[] expected1 = getLongArray(ENTRIES, new Random(87));
    long[] expected2 = getLongArray(ENTRIES, new Random(88));
    // Sanity check for standard addition
    assertEquals(expected1, chunked1);
    assertEquals(expected2, chunked2);

    Random random = new Random(87);
    for (int i = 0 ; i < COPIES ; i++) {
      int startPosA = random.nextInt(ENTRIES);
      int startPosB = random.nextInt(ENTRIES);
      int length = random.nextInt(ENTRIES - Math.max(startPosA, startPosB));
      if (random.nextBoolean()) {
        chunked1.set(chunked2, startPosA, startPosB, length);
        System.arraycopy(expected2, startPosA, expected1, startPosB, length);
      } else {
        chunked2.set(chunked1, startPosA, startPosB, length);
        System.arraycopy(expected1, startPosA, expected2, startPosB, length);
      }
    }
    assertEquals(expected1, chunked1);
    assertEquals(expected2, chunked2);
  }

  private void assertEquals(long[] expected, ChunkedLongArray actual) {
    if (expected.length != actual.size()) {
      fail("Expected array of length " + expected.length
           + " got one of length " + actual.size());
    }
    for (int i = 0 ; i < expected.length ; i++) {
      if (expected[i] != actual.get(i)) {
        fail("The element at index " + i + " was expected to be " + expected[i]
             + " but was " + actual.get(i));
      }
    }
  }
}
