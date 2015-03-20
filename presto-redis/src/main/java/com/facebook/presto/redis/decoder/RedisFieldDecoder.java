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
package com.facebook.presto.redis.decoder;

import com.facebook.presto.redis.RedisColumnHandle;
import com.facebook.presto.redis.RedisFieldValueProvider;

import java.util.Set;

/**
 * Format specific field decoder description.
 */
public interface RedisFieldDecoder<T>
{
    /**
     * Default name. Each decoder type *must* have a default decoder as fallback.
     */
    String DEFAULT_FIELD_DECODER_NAME = "_default";

    /**
     * Returns the types which the field decoder can process.
     */
    Set<Class<?>> getJavaTypes();

    /**
     * Returns the name of the row decoder to which this field decoder belongs.
     */
    String getRowDecoderName();

    /**
     * Returns the field decoder specific name. This name will be selected with the {@link com.facebook.presto.redis.RedisTableFieldDescription#dataFormat} value.
     */
    String getFieldDecoderName();

    /**
     * Decode a value for the given column handle.
     *
     * @param value The raw value as generated by the row decoder.
     * @param columnHandle The column for which the value is decoded.
     * @return A {@link com.facebook.presto.redis.RedisFieldValueProvider} instance which returns a captured value for this specific column.
     */
    RedisFieldValueProvider decode(T value, RedisColumnHandle columnHandle);
}
