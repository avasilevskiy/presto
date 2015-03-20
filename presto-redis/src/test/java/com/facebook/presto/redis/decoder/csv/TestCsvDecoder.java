/*
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
package com.facebook.presto.redis.decoder.csv;

import com.facebook.presto.redis.RedisColumnHandle;
import com.facebook.presto.redis.RedisFieldValueProvider;
import com.facebook.presto.redis.decoder.RedisFieldDecoder;
import com.facebook.presto.redis.decoder.util.DecoderTestUtil;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.BooleanType;
import com.facebook.presto.spi.type.DoubleType;
import com.facebook.presto.spi.type.VarcharType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import static com.facebook.presto.redis.decoder.util.DecoderTestUtil.checkValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TestCsvDecoder
{
    private static final CsvRedisFieldDecoder DEFAULT_FIELD_DECODER = new CsvRedisFieldDecoder();

    private static Map<RedisColumnHandle, RedisFieldDecoder<?>> buildMap(List<RedisColumnHandle> columns)
    {
        ImmutableMap.Builder<RedisColumnHandle, RedisFieldDecoder<?>> map = ImmutableMap.builder();
        for (RedisColumnHandle column : columns) {
            map.put(column, DEFAULT_FIELD_DECODER);
        }
        return map.build();
    }

    @Test
    public void testSimple()
    {
        String csv = "\"row 1\",row2,\"row3\",100,\"200\",300,4.5";

        CsvRedisRowDecoder rowDecoder = new CsvRedisRowDecoder();
        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", VarcharType.VARCHAR, "0", null, null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", VarcharType.VARCHAR, "1", null, null, false, false, false);
        RedisColumnHandle row3 = new RedisColumnHandle("", 2, "row3", VarcharType.VARCHAR, "2", null, null, false, false, false);
        RedisColumnHandle row4 = new RedisColumnHandle("", 3, "row4", BigintType.BIGINT, "3", null, null, false, false, false);
        RedisColumnHandle row5 = new RedisColumnHandle("", 4, "row5", BigintType.BIGINT, "4", null, null, false, false, false);
        RedisColumnHandle row6 = new RedisColumnHandle("", 5, "row6", BigintType.BIGINT, "5", null, null, false, false, false);
        RedisColumnHandle row7 = new RedisColumnHandle("", 6, "row7", DoubleType.DOUBLE, "6", null, null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2, row3, row4, row5, row6, row7);
        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(csv.getBytes(StandardCharsets.UTF_8), null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, "row 1");
        DecoderTestUtil.checkValue(providers, row2, "row2");
        DecoderTestUtil.checkValue(providers, row3, "row3");
        DecoderTestUtil.checkValue(providers, row4, 100);
        DecoderTestUtil.checkValue(providers, row5, 200);
        DecoderTestUtil.checkValue(providers, row6, 300);
        DecoderTestUtil.checkValue(providers, row7, 4.5d);
    }

    @Test
    public void testBoolean()
    {
        String csv = "True,False,0,1,\"0\",\"1\",\"true\",\"false\"";

        CsvRedisRowDecoder rowDecoder = new CsvRedisRowDecoder();

        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", BooleanType.BOOLEAN, "0", null, null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", BooleanType.BOOLEAN, "1", null, null, false, false, false);
        RedisColumnHandle row3 = new RedisColumnHandle("", 2, "row3", BooleanType.BOOLEAN, "2", null, null, false, false, false);
        RedisColumnHandle row4 = new RedisColumnHandle("", 3, "row4", BooleanType.BOOLEAN, "3", null, null, false, false, false);
        RedisColumnHandle row5 = new RedisColumnHandle("", 4, "row5", BooleanType.BOOLEAN, "4", null, null, false, false, false);
        RedisColumnHandle row6 = new RedisColumnHandle("", 5, "row6", BooleanType.BOOLEAN, "5", null, null, false, false, false);
        RedisColumnHandle row7 = new RedisColumnHandle("", 6, "row7", BooleanType.BOOLEAN, "6", null, null, false, false, false);
        RedisColumnHandle row8 = new RedisColumnHandle("", 7, "row8", BooleanType.BOOLEAN, "7", null, null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2, row3, row4, row5, row6, row7, row8);

        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(csv.getBytes(StandardCharsets.UTF_8), null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, true);
        DecoderTestUtil.checkValue(providers, row2, false);
        DecoderTestUtil.checkValue(providers, row3, false);
        DecoderTestUtil.checkValue(providers, row4, false);
        DecoderTestUtil.checkValue(providers, row5, false);
        DecoderTestUtil.checkValue(providers, row6, false);
        DecoderTestUtil.checkValue(providers, row7, true);
        DecoderTestUtil.checkValue(providers, row8, false);
    }

    @Test
    public void testNulls()
    {
        String csv = ",,,";

        CsvRedisRowDecoder rowDecoder = new CsvRedisRowDecoder();

        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", VarcharType.VARCHAR, "0", null, null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", BigintType.BIGINT, "1", null, null, false, false, false);
        RedisColumnHandle row3 = new RedisColumnHandle("", 2, "row3", DoubleType.DOUBLE, "2", null, null, false, false, false);
        RedisColumnHandle row4 = new RedisColumnHandle("", 3, "row4", BooleanType.BOOLEAN, "3", null, null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2, row3, row4);

        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(csv.getBytes(StandardCharsets.UTF_8), null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, "");
        DecoderTestUtil.checkValue(providers, row2, 0);
        DecoderTestUtil.checkValue(providers, row3, 0.0d);
        DecoderTestUtil.checkValue(providers, row4, false);
    }
}
