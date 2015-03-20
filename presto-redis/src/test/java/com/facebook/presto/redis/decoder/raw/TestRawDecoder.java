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
package com.facebook.presto.redis.decoder.raw;

import com.facebook.presto.redis.RedisColumnHandle;
import com.facebook.presto.redis.RedisFieldValueProvider;
import com.facebook.presto.redis.decoder.RedisFieldDecoder;
import com.facebook.presto.redis.decoder.util.DecoderTestUtil;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.BooleanType;
import com.facebook.presto.spi.type.VarcharType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import static com.facebook.presto.redis.decoder.util.DecoderTestUtil.checkValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class TestRawDecoder
{
    private static final RawRedisFieldDecoder DEFAULT_FIELD_DECODER = new RawRedisFieldDecoder();

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
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.putLong(4815162342L); // 0 - 7
        buf.putInt(12345678); // 8 - 11
        buf.putShort((short) 4567); // 12 - 13
        buf.put((byte) 123); // 14
        buf.put("Ich bin zwei Oeltanks".getBytes(StandardCharsets.UTF_8)); // 15+

        byte[] row = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, row, 0, buf.position());

        RawRedisRowDecoder rowDecoder = new RawRedisRowDecoder();
        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", BigintType.BIGINT, "0", "LONG", null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", BigintType.BIGINT, "8", "INT", null, false, false, false);
        RedisColumnHandle row3 = new RedisColumnHandle("", 2, "row3", BigintType.BIGINT, "12", "SHORT", null, false, false, false);
        RedisColumnHandle row4 = new RedisColumnHandle("", 3, "row4", BigintType.BIGINT, "14", "BYTE", null, false, false, false);
        RedisColumnHandle row5 = new RedisColumnHandle("", 4, "row5", VarcharType.VARCHAR, "15", null, null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2, row3, row4, row5);
        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(row, null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, 4815162342L);
        DecoderTestUtil.checkValue(providers, row2, 12345678);
        DecoderTestUtil.checkValue(providers, row3, 4567);
        DecoderTestUtil.checkValue(providers, row4, 123);
        DecoderTestUtil.checkValue(providers, row5, "Ich bin zwei Oeltanks");
    }

    @Test
    public void testFixedWithString()
    {
        String str = "Ich bin zwei Oeltanks";
        byte[] row = str.getBytes(StandardCharsets.UTF_8);

        RawRedisRowDecoder rowDecoder = new RawRedisRowDecoder();
        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", VarcharType.VARCHAR, null, null, null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", VarcharType.VARCHAR, "0", null, null, false, false, false);
        RedisColumnHandle row3 = new RedisColumnHandle("", 2, "row3", VarcharType.VARCHAR, "0:4", null, null, false, false, false);
        RedisColumnHandle row4 = new RedisColumnHandle("", 3, "row4", VarcharType.VARCHAR, "5:8", null, null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2, row3, row4);
        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(row, null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, str);
        DecoderTestUtil.checkValue(providers, row2, str);
        // these only work for single byte encodings...
        DecoderTestUtil.checkValue(providers, row3, str.substring(0, 4));
        DecoderTestUtil.checkValue(providers, row4, str.substring(5, 8));
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Test
    public void testFloatStuff()
    {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.putDouble(Math.PI);
        buf.putFloat((float) Math.E);
        buf.putDouble(Math.E);

        byte[] row = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, row, 0, buf.position());

        RawRedisRowDecoder rowDecoder = new RawRedisRowDecoder();
        RedisColumnHandle row1 = new RedisColumnHandle("", 0, "row1", VarcharType.VARCHAR, null, "DOUBLE", null, false, false, false);
        RedisColumnHandle row2 = new RedisColumnHandle("", 1, "row2", VarcharType.VARCHAR, "8", "FLOAT", null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row1, row2);
        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(row, null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row1, Math.PI);
        DecoderTestUtil.checkValue(providers, row2, Math.E);
    }

    @Test
    public void testBooleanStuff()
    {
        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.put((byte) 127); // offset 0
        buf.putLong(0); // offset 1
        buf.put((byte) 126); // offset 9
        buf.putLong(1); // offset 10

        buf.put((byte) 125); // offset 18
        buf.putInt(0); // offset 19
        buf.put((byte) 124); // offset 23
        buf.putInt(1); // offset 24

        buf.put((byte) 123); // offset 28
        buf.putShort((short) 0); // offset 29
        buf.put((byte) 122); // offset 31
        buf.putShort((short) 1); // offset 32

        buf.put((byte) 121); // offset 34
        buf.put((byte) 0); // offset 35
        buf.put((byte) 120); // offset 36
        buf.put((byte) 1); // offset 37

        byte[] row = new byte[buf.position()];
        System.arraycopy(buf.array(), 0, row, 0, buf.position());

        RawRedisRowDecoder rowDecoder = new RawRedisRowDecoder();
        RedisColumnHandle row01 = new RedisColumnHandle("", 0, "row01", BigintType.BIGINT, "0", "BYTE", null, false, false, false);
        RedisColumnHandle row02 = new RedisColumnHandle("", 1, "row02", BooleanType.BOOLEAN, "1", "LONG", null, false, false, false);
        RedisColumnHandle row03 = new RedisColumnHandle("", 2, "row03", BigintType.BIGINT, "9", "BYTE", null, false, false, false);
        RedisColumnHandle row04 = new RedisColumnHandle("", 3, "row04", BooleanType.BOOLEAN, "10", "LONG", null, false, false, false);

        RedisColumnHandle row11 = new RedisColumnHandle("", 4, "row11", BigintType.BIGINT, "18", "BYTE", null, false, false, false);
        RedisColumnHandle row12 = new RedisColumnHandle("", 5, "row12", BooleanType.BOOLEAN, "19", "INT", null, false, false, false);
        RedisColumnHandle row13 = new RedisColumnHandle("", 6, "row13", BigintType.BIGINT, "23", "BYTE", null, false, false, false);
        RedisColumnHandle row14 = new RedisColumnHandle("", 7, "row14", BooleanType.BOOLEAN, "24", "INT", null, false, false, false);

        RedisColumnHandle row21 = new RedisColumnHandle("", 8, "row21", BigintType.BIGINT, "28", "BYTE", null, false, false, false);
        RedisColumnHandle row22 = new RedisColumnHandle("", 9, "row22", BooleanType.BOOLEAN, "29", "SHORT", null, false, false, false);
        RedisColumnHandle row23 = new RedisColumnHandle("", 10, "row23", BigintType.BIGINT, "31", "BYTE", null, false, false, false);
        RedisColumnHandle row24 = new RedisColumnHandle("", 11, "row24", BooleanType.BOOLEAN, "32", "SHORT", null, false, false, false);

        RedisColumnHandle row31 = new RedisColumnHandle("", 12, "row31", BigintType.BIGINT, "34", "BYTE", null, false, false, false);
        RedisColumnHandle row32 = new RedisColumnHandle("", 13, "row32", BooleanType.BOOLEAN, "35", "BYTE", null, false, false, false);
        RedisColumnHandle row33 = new RedisColumnHandle("", 14, "row33", BigintType.BIGINT, "36", "BYTE", null, false, false, false);
        RedisColumnHandle row34 = new RedisColumnHandle("", 15, "row34", BooleanType.BOOLEAN, "37", "BYTE", null, false, false, false);

        List<RedisColumnHandle> columns = ImmutableList.of(row01,
                row02,
                row03,
                row04,
                row11,
                row12,
                row13,
                row14,
                row21,
                row22,
                row23,
                row24,
                row31,
                row32,
                row33,
                row34);

        Set<RedisFieldValueProvider> providers = new HashSet<>();

        boolean corrupt = rowDecoder.decodeRow(row, null, providers, columns, buildMap(columns));
        assertFalse(corrupt);

        assertEquals(providers.size(), columns.size());

        DecoderTestUtil.checkValue(providers, row01, 127);
        DecoderTestUtil.checkValue(providers, row02, false);
        DecoderTestUtil.checkValue(providers, row03, 126);
        DecoderTestUtil.checkValue(providers, row04, true);

        DecoderTestUtil.checkValue(providers, row11, 125);
        DecoderTestUtil.checkValue(providers, row12, false);
        DecoderTestUtil.checkValue(providers, row13, 124);
        DecoderTestUtil.checkValue(providers, row14, true);

        DecoderTestUtil.checkValue(providers, row21, 123);
        DecoderTestUtil.checkValue(providers, row22, false);
        DecoderTestUtil.checkValue(providers, row23, 122);
        DecoderTestUtil.checkValue(providers, row24, true);

        DecoderTestUtil.checkValue(providers, row31, 121);
        DecoderTestUtil.checkValue(providers, row32, false);
        DecoderTestUtil.checkValue(providers, row33, 120);
        DecoderTestUtil.checkValue(providers, row34, true);
    }
}
