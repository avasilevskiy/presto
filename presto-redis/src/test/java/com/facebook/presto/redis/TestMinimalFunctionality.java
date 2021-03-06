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
package com.facebook.presto.redis;

import com.facebook.presto.Session;
import com.facebook.presto.redis.util.JsonEncoder;
import com.facebook.presto.redis.util.TestUtils;
import com.facebook.presto.metadata.QualifiedTableName;
import com.facebook.presto.metadata.TableHandle;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.testing.MaterializedResult;
import com.facebook.presto.tests.StandaloneQueryRunner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;

import com.facebook.presto.redis.util.EmbeddedRedis;
import static com.facebook.presto.spi.type.TimeZoneKey.UTC_KEY;
import static java.util.Locale.ENGLISH;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(singleThreaded = true)
public class TestMinimalFunctionality
{
    private static final Session SESSION = Session.builder()
            .setUser("user")
            .setSource("source")
            .setCatalog("redis")
            .setSchema("default")
            .setTimeZoneKey(UTC_KEY)
            .setLocale(ENGLISH)
            .build();

    private EmbeddedRedis embeddedRedis;
    private String tableName;
    private StandaloneQueryRunner queryRunner;

    @BeforeClass
    public void startRedis()
            throws Exception
    {
        embeddedRedis = EmbeddedRedis.createEmbeddedRedis();
        embeddedRedis.start();
    }

    @AfterClass
    public void stopRedis()
            throws Exception
    {
        embeddedRedis.close();
    }

    @BeforeMethod
    public void spinUp()
            throws Exception
    {
        this.tableName = "test_" + UUID.randomUUID().toString().replaceAll("-", "_");

        this.queryRunner = new StandaloneQueryRunner(SESSION);

        TestUtils.installRedisPlugin(embeddedRedis, queryRunner,
                ImmutableMap.<SchemaTableName, RedisTableDescription>builder()
                        .put(TestUtils.createEmptyTableDescription(new SchemaTableName("default", tableName)))
                        .build());
    }

    @AfterMethod
    public void tearDown()
            throws Exception
    {
        queryRunner.close();
    }

    private void populateData(int count)
    {
        JsonEncoder jsonEncoder = new JsonEncoder();
        try {
            for (long i = 0; i < count; i++) {
                Object value = ImmutableMap.of("id", Long.toString(i), "value", UUID.randomUUID().toString());

                try (Jedis jedis = embeddedRedis.getJedisPool().getResource()) {
                    jedis.set(tableName + ":" + i,
                            jsonEncoder.toString(value));
                }
            }
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Test
    public void testTableExists()
            throws Exception
    {
        QualifiedTableName name = new QualifiedTableName("redis", "default", tableName);
        Optional<TableHandle> handle = queryRunner.getServer().getMetadata().getTableHandle(SESSION, name);
        assertTrue(handle.isPresent());
    }

    @Test
    public void testTableHasData()
            throws Exception
    {
        MaterializedResult result = queryRunner.execute("SELECT count(1) from " + tableName);

        MaterializedResult expected = MaterializedResult.resultBuilder(SESSION, BigintType.BIGINT)
                .row(0)
                .build();

        assertEquals(result, expected);

        int count = 1000;
        populateData(count);

        result = queryRunner.execute("SELECT count(1) from " + tableName);

        expected = MaterializedResult.resultBuilder(SESSION, BigintType.BIGINT)
                .row(count)
                .build();

        assertEquals(result, expected);
    }
}
