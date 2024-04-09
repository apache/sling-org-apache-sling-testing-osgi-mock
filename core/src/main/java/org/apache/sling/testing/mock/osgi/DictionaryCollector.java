/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.osgi;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class DictionaryCollector<K, V> implements Collector<Entry<K, V>, Hashtable<K, V>, Dictionary<K, V>> {

    private final Function<? super Entry<K, V>, ? extends K> keyMapper;
    private final Function<? super Entry<K, V>, ? extends V> valueMapper;

    public DictionaryCollector(
            Function<? super Entry<K, V>, ? extends K> keyMapper,
            Function<? super Entry<K, V>, ? extends V> valueMapper) {
        super();
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @Override
    public Supplier<Hashtable<K, V>> supplier() {
        return Hashtable::new;
    }

    @Override
    public BiConsumer<Hashtable<K, V>, Entry<K, V>> accumulator() {
        return (hashTable, entry) -> {
            if (valueMapper.apply(entry) != null) {
                hashTable.put(keyMapper.apply(entry), valueMapper.apply(entry));
            }
        };
    }

    @Override
    public BinaryOperator<Hashtable<K, V>> combiner() {
        return (dictionary1, dictionary2) -> {
            dictionary1.putAll(dictionary2);
            return dictionary1;
        };
    }

    @Override
    public Function<Hashtable<K, V>, Dictionary<K, V>> finisher() {
        return table -> table;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.UNORDERED);
    }
}
