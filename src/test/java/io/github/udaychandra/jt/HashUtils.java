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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtils {

  private HashUtils() {}

  public static String sha3Hash(String pValue)
      throws NoSuchAlgorithmException {
    final var digest = MessageDigest.getInstance("SHA3-256");
    final var hashbytes = digest.digest(pValue.getBytes(StandardCharsets.UTF_8));
    return new String(Base64.getEncoder().encode(hashbytes), StandardCharsets.UTF_8);
  }
}
