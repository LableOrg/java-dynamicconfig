/*
 * Copyright © 2015 Lable (info@lable.nl)
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
package org.lable.oss.dynamicconfig.provider.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class EtcdTestUtil {
    public static void put(Client etcd, String key, String value) throws ExecutionException, InterruptedException {
        etcd.getKVClient().put(byteSequenceFromString(key), byteSequenceFromString(value)).get();
    }

    public static void delete(Client etcd, String key) throws ExecutionException, InterruptedException {
        etcd.getKVClient().delete(byteSequenceFromString(key)).get();
    }

    public static ByteSequence byteSequenceFromString(String value) {
        return ByteSequence.from(value, StandardCharsets.UTF_8);
    }
}
