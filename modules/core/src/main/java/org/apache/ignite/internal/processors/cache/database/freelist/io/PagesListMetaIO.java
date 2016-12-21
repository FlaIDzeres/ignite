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

package org.apache.ignite.internal.processors.cache.database.freelist.io;

import java.util.Map;
import org.apache.ignite.internal.pagemem.PageUtils;
import org.apache.ignite.internal.processors.cache.database.freelist.PagesList;
import org.apache.ignite.internal.processors.cache.database.tree.io.IOVersions;
import org.apache.ignite.internal.processors.cache.database.tree.io.PageIO;
import org.apache.ignite.internal.util.GridLongList;

/**
 *
 */
public class PagesListMetaIO extends PageIO {
    /** */
    private static final int CNT_OFF = COMMON_HEADER_END;

    /** */
    private static final int NEXT_META_PAGE_OFF = CNT_OFF + 2;

    /** */
    private static final int ITEMS_OFF = NEXT_META_PAGE_OFF + 8;

    /** */
    private static final int ITEM_SIZE = 10;

    /** */
    public static final IOVersions<PagesListMetaIO> VERSIONS = new IOVersions<>(
        new PagesListMetaIO(1)
    );

    /**
     * @param ver  Page format version.
     */
    private PagesListMetaIO(int ver) {
        super(T_PAGE_LIST_META, ver);
    }

    /** {@inheritDoc} */
    @Override public void initNewPage(long buf, long pageId) {
        super.initNewPage(buf, pageId);

        setCount(buf, 0);
        setNextMetaPageId(buf, 0L);
    }

    /**
     * @param buf Buffer.
     * @return Stored items count.
     */
    private int getCount(long buf) {
        return PageUtils.getShort(buf, CNT_OFF);
    }

    /**
     * @param buf Buffer,
     * @param cnt Stored items count.
     */
    private void setCount(long buf, int cnt) {
        assert cnt >= 0 && cnt <= Short.MAX_VALUE : cnt;

        PageUtils.putShort(buf, CNT_OFF, (short)cnt);
    }

    /**
     * @param buf Buffer.
     * @return Next meta page ID.
     */
    public long getNextMetaPageId(long buf) {
        return PageUtils.getLong(buf, NEXT_META_PAGE_OFF);
    }

    /**
     * @param buf Buffer.
     * @param metaPageId Next meta page ID.
     */
    public void setNextMetaPageId(long buf, long metaPageId) {
        PageUtils.putLong(buf, NEXT_META_PAGE_OFF, metaPageId);
    }

    /**
     * @param buf Buffer.
     */
    public void resetCount(long buf) {
        setCount(buf, 0);
    }

    /**
     * @param pageSize Page size.
     * @param buf Buffer.
     * @param bucket Bucket number.
     * @param tails Tails.
     * @param tailsOff Tails offset.
     * @return Number of items written.
     */
    public int addTails(int pageSize, long buf, int bucket, PagesList.Stripe[] tails, int tailsOff) {
        assert bucket >= 0 && bucket <= Short.MAX_VALUE : bucket;

        int cnt = getCount(buf);
        int cap = getCapacity(pageSize, buf);

        if (cnt == cap)
            return 0;

        int off = offset(cnt);

        int write = Math.min(cap - cnt, tails.length - tailsOff);

        for (int i = 0; i < write; i++) {
            PageUtils.putShort(buf, off, (short)bucket);
            PageUtils.putLong(buf, off + 2, tails[tailsOff].tailId);

            tailsOff++;

            off += ITEM_SIZE;
        }

        setCount(buf, cnt + write);

        return write;
    }

    /**
     * @param buf Buffer.
     * @param res Results map.
     */
    public void getBucketsData(long buf, Map<Integer, GridLongList> res) {
        int cnt = getCount(buf);

        assert cnt >= 0 && cnt <= Short.MAX_VALUE : cnt;

        if (cnt == 0)
            return;

        int off = offset(0);

        for (int i = 0; i < cnt; i++) {
            Integer bucket = (int)PageUtils.getShort(buf, off);
            assert bucket >= 0 && bucket <= Short.MAX_VALUE : bucket;

            long tailId = PageUtils.getLong(buf, off + 2);
            assert tailId != 0;

            GridLongList list = res.get(bucket);

            if (list == null)
                res.put(bucket, list = new GridLongList());

            list.add(tailId);

            off += ITEM_SIZE;
        }
    }

    /**
     * @param buf Buffer.
     * @return Maximum number of items which can be stored in buffer.
     */
    private int getCapacity(int pageSize, long buf) {
        return (pageSize - ITEMS_OFF) / ITEM_SIZE;
    }

    /**
     * @param idx Item index.
     * @return Item offset.
     */
    private int offset(int idx) {
        return ITEMS_OFF + ITEM_SIZE * idx;
    }
}
