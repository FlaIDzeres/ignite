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

package org.apache.ignite.internal.processors.cache.database.tree.io;

import org.apache.ignite.internal.pagemem.PageUtils;

/**
 * IO routines for B+Tree meta pages.
 */
public class BPlusMetaIO extends PageIO {
    /** */
    public static final IOVersions<BPlusMetaIO> VERSIONS = new IOVersions<>(
        new BPlusMetaIO(1)
    );

    /** */
    private static final int LVLS_OFF = COMMON_HEADER_END;

    /** */
    private static final int REFS_OFF = LVLS_OFF + 1;

    /**
     * @param ver Page format version.
     */
    protected BPlusMetaIO(int ver) {
        super(T_BPLUS_META, ver);
    }

    /**
     * @param buf Buffer.
     * @param rootId Root page ID.
     */
    public void initRoot(int pageSize, long buf, long rootId) {
        setLevelsCount(pageSize, buf, 1);
        setFirstPageId(buf, 0, rootId);
    }

    /**
     * @param buf Buffer.
     * @return Number of levels in this tree.
     */
    public int getLevelsCount(long buf) {
        return PageUtils.getByte(buf, LVLS_OFF);
    }

    /**
     * @param buf Buffer.
     * @return Max levels possible for this page size.
     */
    public int getMaxLevels(int pageSize, long buf) {
        return (pageSize - REFS_OFF) / 8;
    }

    /**
     * @param buf  Buffer.
     * @param lvls Number of levels in this tree.
     */
    public void setLevelsCount(int pageSize, long buf, int lvls) {
        assert lvls >= 0 && lvls <= getMaxLevels(pageSize, buf) : lvls;

        PageUtils.putByte(buf, LVLS_OFF, (byte)lvls);

        assert getLevelsCount(buf) == lvls;
    }

    /**
     * @param lvl Level.
     * @return Offset for page reference.
     */
    private static int offset(int lvl) {
        return lvl * 8 + REFS_OFF;
    }

    /**
     * @param buf Buffer.
     * @param lvl Level.
     * @return First page ID at that level.
     */
    public long getFirstPageId(long buf, int lvl) {
        return PageUtils.getLong(buf, offset(lvl));
    }

    /**
     * @param buf    Buffer.
     * @param lvl    Level.
     * @param pageId Page ID.
     */
    public void setFirstPageId(long buf, int lvl, long pageId) {
        assert lvl >= 0 && lvl < getLevelsCount(buf);

        PageUtils.putLong(buf, offset(lvl), pageId);

        assert getFirstPageId(buf, lvl) == pageId;
    }

    /**
     * @param buf Buffer.
     * @return Root level.
     */
    public int getRootLevel(long buf) {
        int lvls = getLevelsCount(buf); // The highest level page is root.

        assert lvls > 0 : lvls;

        return lvls - 1;
    }

    /**
     * @param buf Buffer.
     * @param rootPageId New root page ID.
     */
    public void addRoot(int pageSize, long buf, long rootPageId) {
        int lvl = getLevelsCount(buf);

        setLevelsCount(pageSize, buf, lvl + 1);
        setFirstPageId(buf, lvl, rootPageId);
    }

    /**
     * @param buf Buffer.
     */
    public void cutRoot(int pageSize, long buf) {
        int lvl = getRootLevel(buf);

        setLevelsCount(pageSize, buf, lvl); // Decrease tree height.
    }
}
