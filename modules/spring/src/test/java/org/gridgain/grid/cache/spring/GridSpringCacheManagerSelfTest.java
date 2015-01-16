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

package org.gridgain.grid.cache.spring;

import org.apache.ignite.cache.*;
import org.apache.ignite.configuration.*;
import org.gridgain.grid.cache.*;
import org.apache.ignite.spi.discovery.tcp.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.*;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;
import org.springframework.beans.factory.*;
import org.springframework.context.support.*;

/**
 * Spring cache test.
 */
public class GridSpringCacheManagerSelfTest extends GridCommonAbstractTest {
    /** */
    private static final TcpDiscoveryIpFinder IP_FINDER = new TcpDiscoveryVmIpFinder(true);

    /** */
    private static final String CACHE_NAME = "testCache";

    /** */
    private GridSpringCacheTestService svc;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        CacheConfiguration cache = new CacheConfiguration();

        cache.setName(CACHE_NAME);

        cfg.setCacheConfiguration(cache);

        TcpDiscoverySpi disco = new TcpDiscoverySpi();

        disco.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(disco);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override public String getTestGridName() {
        return "testGrid";
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        startGrid();
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        BeanFactory factory = new ClassPathXmlApplicationContext("org/gridgain/grid/cache/spring/spring-caching.xml");

        svc = (GridSpringCacheTestService)factory.getBean("testService");

        svc.reset();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        grid().cache(CACHE_NAME).removeAll();
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleKey() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals("value" + i, svc.simpleKey(i));
            assertEquals("value" + i, svc.simpleKey(i));
        }

        assertEquals(3, svc.called());

        GridCache<Integer, String> c = grid().cache(CACHE_NAME);

        assertEquals(3, c.size());

        for (int i = 0; i < 3; i++)
            assertEquals("value" + i, c.get(i));
    }

    /**
     * @throws Exception If failed.
     */
    public void testComplexKey() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals("value" + i + "suffix" + i, svc.complexKey(i, "suffix" + i));
            assertEquals("value" + i + "suffix" + i, svc.complexKey(i, "suffix" + i));
        }

        assertEquals(3, svc.called());

        GridCache<GridSpringCacheTestKey, String> c = grid().cache(CACHE_NAME);

        assertEquals(3, c.size());

        for (int i = 0; i < 3; i++)
            assertEquals("value" + i + "suffix" + i, c.get(new GridSpringCacheTestKey(i, "suffix" + i)));
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleKeyPut() throws Exception {
        GridCache<Integer, String> c = grid().cache(CACHE_NAME);

        for (int i = 0; i < 3; i++) {
            assertEquals("value" + i + "odd", svc.simpleKeyPut(i));

            assertEquals(i + 1, c.size());
            assertEquals("value" + i + "odd", c.get(i));

            assertEquals("value" + i + "even", svc.simpleKeyPut(i));

            assertEquals(i + 1, c.size());
            assertEquals("value" + i + "even", c.get(i));
        }

        assertEquals(6, svc.called());
    }

    /**
     * @throws Exception If failed.
     */
    public void testComplexKeyPut() throws Exception {
        GridCache<GridSpringCacheTestKey, String> c = grid().cache(CACHE_NAME);

        for (int i = 0; i < 3; i++) {
            assertEquals("value" + i + "suffix" + i + "odd", svc.complexKeyPut(i, "suffix" + i));

            assertEquals(i + 1, c.size());
            assertEquals("value" + i + "suffix" + i + "odd", c.get(new GridSpringCacheTestKey(i, "suffix" + i)));

            assertEquals("value" + i + "suffix" + i + "even", svc.complexKeyPut(i, "suffix" + i));

            assertEquals(i + 1, c.size());
            assertEquals("value" + i + "suffix" + i + "even", c.get(new GridSpringCacheTestKey(i, "suffix" + i)));
        }

        assertEquals(6, svc.called());
    }

    /**
     * @throws Exception If failed.
     */
    public void testSimpleKeyEvict() throws Exception {
        GridCache<Integer, String> c = grid().cache(CACHE_NAME);

        for (int i = 0; i < 3; i++)
            c.putx(i, "value" + i);

        assertEquals(3, c.size());

        assertEquals("value0", c.get(0));
        assertEquals("value1", c.get(1));
        assertEquals("value2", c.get(2));

        svc.simpleKeyEvict(2);

        assertEquals(2, c.size());

        assertEquals("value0", c.get(0));
        assertEquals("value1", c.get(1));
        assertNull(c.get(2));
    }

    /**
     * @throws Exception If failed.
     */
    public void testComplexKeyEvict() throws Exception {
        GridCache<GridSpringCacheTestKey, String> c = grid().cache(CACHE_NAME);

        for (int i = 0; i < 3; i++)
            c.putx(new GridSpringCacheTestKey(i, "suffix" + i), "value" + i);

        assertEquals(3, c.size());

        assertEquals("value0", c.get(new GridSpringCacheTestKey(0, "suffix" + 0)));
        assertEquals("value1", c.get(new GridSpringCacheTestKey(1, "suffix" + 1)));
        assertEquals("value2", c.get(new GridSpringCacheTestKey(2, "suffix" + 2)));

        svc.complexKeyEvict(2, "suffix" + 2);

        assertEquals(2, c.size());

        assertEquals("value0", c.get(new GridSpringCacheTestKey(0, "suffix" + 0)));
        assertEquals("value1", c.get(new GridSpringCacheTestKey(1, "suffix" + 1)));
        assertNull(c.get(new GridSpringCacheTestKey(2, "suffix" + 2)));
    }

    /**
     * @throws Exception If failed.
     */
    public void testEvictAll() throws Exception {
        GridCache<Integer, String> c = grid().cache(CACHE_NAME);

        for (int i = 0; i < 3; i++)
            c.putx(i, "value" + i);

        assertEquals(3, c.size());

        assertEquals("value0", c.get(0));
        assertEquals("value1", c.get(1));
        assertEquals("value2", c.get(2));

        svc.evictAll();

        assertEquals(0, c.size());
    }
}
