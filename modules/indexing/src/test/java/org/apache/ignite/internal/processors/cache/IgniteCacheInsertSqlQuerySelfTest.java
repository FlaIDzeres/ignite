/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache;

import java.util.concurrent.Callable;
import javax.cache.CacheException;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.GridTestUtils;

/**
 *
 */
@SuppressWarnings("unchecked")
public class IgniteCacheInsertSqlQuerySelfTest extends IgniteCacheAbstractInsertSqlQuerySelfTest {
    /** */
    private static final TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setPeerClassLoadingEnabled(false);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /**
     *
     */
    public void testInsertWithExplicitKey() {
        IgniteCache<String, Person> p = ignite(0).cache("S2P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into Person (_key, id, firstName) values ('s', ?, ?), " +
            "('a', 2, 'Alex')").setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get("s"));

        assertEquals(createPerson(2, "Alex"), p.get("a"));
    }

    /**
     *
     */
    public void testInsertFromSubquery() {
        IgniteCache p = ignite(0).cache("S2P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into String (_key, _val) values ('s', ?), " +
            "('a', ?)").setArgs("Sergi", "Alex").setLocal(true));

        assertEquals("Sergi", p.get("s"));
        assertEquals("Alex", p.get("a"));

        p.query(new SqlFieldsQuery("insert into Person(_key, id, firstName) " +
            "(select substring(lower(_val), 0, 2), cast(length(_val) as int), _val from String)"));

        assertEquals(createPerson(5, "Sergi"), p.get("se"));

        assertEquals(createPerson(4, "Alex"), p.get("al"));
    }

    /**
     *
     */
    public void testInsertWithExplicitPrimitiveKey() {
        IgniteCache<Integer, Person> p = ignite(0).cache("I2P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (_key, id, firstName) values (cast('1' as int), ?, ?), (2, (5 - 3), 'Alex')")
            .setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(1));

        assertEquals(createPerson(2, "Alex"), p.get(2));
    }

    /**
     *
     */
    public void testInsertWithDynamicKeyInstantiation() {
        IgniteCache<Key, Person> p = ignite(0).cache("K2P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (key, id, firstName) values (1, ?, ?), (2, 2, 'Alex')").setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(new Key(1)));

        assertEquals(createPerson(2, "Alex"), p.get(new Key(2)));
    }

    /**
     *
     */
    public void testFieldsCaseSensitivity() {
        IgniteCache<Key2, Person> p = ignite(0).cache("K22P").withKeepBinary();

        p.query(new SqlFieldsQuery("insert into \"Person2\" (\"Id\", \"id\", \"firstName\", \"IntVal\") " +
            "values (1, ?, ?, 5),  (2, 3, 'Alex', 6)").setArgs(4, "Sergi"));

        assertEquals(createPerson2(4, "Sergi", 5), p.get(new Key2(1)));

        assertEquals(createPerson2(3, "Alex", 6), p.get(new Key2(2)));
    }

    /**
     *
     */
    public void testPrimitives() {
        IgniteCache<Integer, Integer> p = ignite(0).cache("I2I");

        p.query(new SqlFieldsQuery("insert into Integer(_key, _val) values (1, ?), " +
            "(?, 4)").setArgs(2, 3));

        assertEquals(2, (int)p.get(1));

        assertEquals(4, (int)p.get(3));
    }

    /**
     *
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testDuplicateKeysException() {
        final IgniteCache<Integer, Integer> p = ignite(0).cache("I2I");

        p.clear();

        p.put(3, 5);

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            /** {@inheritDoc} */
            @Override public Void call() throws Exception {
                p.query(new SqlFieldsQuery("insert into Integer(_key, _val) values (1, ?), " +
                    "(?, 4), (5, 6)").setArgs(2, 3));

                return null;
            }
        }, CacheException.class, "Failed to INSERT some keys because they are already in cache [keys=[3]]");

        assertEquals(2, (int)p.get(1));
        assertEquals(5, (int)p.get(3));
        assertEquals(6, (int)p.get(5));
    }

    /**
     *
     */
    public void testFieldsListIdentity() {
        if (!isBinaryMarshaller())
            return;

        IgniteCache<Key3, Person> p = ignite(0).cache("K32P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (key, strKey, id, firstName) values (1, 'aa', ?, ?), (2, 'bb', 2, 'Alex')")
            .setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(new Key3(1)));

        assertEquals(createPerson(2, "Alex"), p.get(new Key3(2)));
    }

    /**
     *
     */
    public void testCustomIdentity() {
        if (!isBinaryMarshaller())
            return;

        IgniteCache<Key4, Person> p = ignite(0).cache("K42P").withKeepBinary();

        p.query(new SqlFieldsQuery(
            "insert into Person (key, strKey, id, firstName) values (1, 'aa', ?, ?), (2, 'bb', 2, 'Alex')")
            .setArgs(1, "Sergi"));

        assertEquals(createPerson(1, "Sergi"), p.get(new Key4(1)));

        assertEquals(createPerson(2, "Alex"), p.get(new Key4(2)));
    }
}
