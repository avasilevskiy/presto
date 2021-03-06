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
package com.facebook.presto.redis.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

public class EmbeddedRedis
        implements Closeable
{
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private JedisPool jedisPool = null;
    private RedisServer redisServer;
    public static EmbeddedRedis createEmbeddedRedis()
            throws IOException, URISyntaxException
    {
        return new EmbeddedRedis();
    }

    EmbeddedRedis()
            throws IOException, URISyntaxException
    {
        redisServer = new RedisServer();
    }

    public void start()
            throws InterruptedException, IOException
    {
        redisServer.start();
        jedisPool = new JedisPool(new JedisPoolConfig(), getConnectString(), getPort());
    }
    public JedisPool getJedisPool()
    {
       return jedisPool;
    }

    public void destroyJedisPool()
    {
        jedisPool.destroy();
    }

    @Override
    public void close()
    {
        jedisPool.destroy();
        try {
            redisServer.stop();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public int getPort()
    {
        return redisServer.getPort();
    }
    public String getConnectString()
    {
        return "localhost";
    }
}
