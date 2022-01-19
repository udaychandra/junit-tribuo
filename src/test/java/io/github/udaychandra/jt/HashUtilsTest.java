/*
 * Copyright 2022 Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.udaychandra.jt;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@AnomalyDetector
public class HashUtilsTest {

  @Test
  public void sha3HashTest()
      throws NoSuchAlgorithmException {
    var valueToHash = "junit with tribuo is fun";
    var hash = HashUtils.sha3Hash(valueToHash);
    assertEquals("WFW/VjUqR0lJKGhQ5VpC42tZX9nplnc1jIHJPX+/Jv0=", hash);
  }
}
