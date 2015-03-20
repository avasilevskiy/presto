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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import io.airlift.log.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.facebook.presto.redis.decoder.RedisFieldDecoder.DEFAULT_FIELD_DECODER_NAME;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Manages Row and Field decoders for the various dataFormat values.
 */
public class RedisDecoderRegistry
{
    private static final Logger log = Logger.get(RedisDecoderRegistry.class);

    private final Map<String, RedisRowDecoder> rowDecoders;
    private final Map<String, SetMultimap<Class<?>, RedisFieldDecoder<?>>> fieldDecoders;

    @Inject
    RedisDecoderRegistry(Set<RedisRowDecoder> rowDecoders,
                         Set<RedisFieldDecoder<?>> fieldDecoders)
    {
        checkNotNull(rowDecoders, "rowDecoders is null");

        ImmutableMap.Builder<String, RedisRowDecoder> rowBuilder = ImmutableMap.builder();
        for (RedisRowDecoder rowDecoder : rowDecoders) {
            rowBuilder.put(rowDecoder.getName(), rowDecoder);
        }

        this.rowDecoders = rowBuilder.build();

        Map<String, ImmutableSetMultimap.Builder<Class<?>, RedisFieldDecoder<?>>> fieldDecoderBuilders = new HashMap<>();

        for (RedisFieldDecoder<?> fieldDecoder : fieldDecoders) {
            ImmutableSetMultimap.Builder<Class<?>, RedisFieldDecoder<?>> fieldDecoderBuilder = fieldDecoderBuilders.get(fieldDecoder.getRowDecoderName());
            if (fieldDecoderBuilder == null) {
                fieldDecoderBuilder = ImmutableSetMultimap.builder();
                fieldDecoderBuilders.put(fieldDecoder.getRowDecoderName(), fieldDecoderBuilder);
            }

            for (Class<?> clazz : fieldDecoder.getJavaTypes()) {
                fieldDecoderBuilder.put(clazz, fieldDecoder);
            }
        }

        ImmutableMap.Builder<String, SetMultimap<Class<?>, RedisFieldDecoder<?>>> fieldDecoderBuilder = ImmutableMap.builder();
        for (Map.Entry<String, ImmutableSetMultimap.Builder<Class<?>, RedisFieldDecoder<?>>> entry : fieldDecoderBuilders.entrySet()) {
            fieldDecoderBuilder.put(entry.getKey(), entry.getValue().build());
        }

        this.fieldDecoders = fieldDecoderBuilder.build();
        log.debug("Field decoders found: %s", this.fieldDecoders);
    }

    /**
     * Return the specific row decoder for a given data format.
     */
    public RedisRowDecoder getRowDecoder(String dataFormat)
    {
        checkState(rowDecoders.containsKey(dataFormat), "no row decoder for '%s' found", dataFormat);
        return rowDecoders.get(dataFormat);
    }

    /**
     * Return the best matching field decoder for a given row data format, field type and a possible field data format name. If no
     * name was given or an unknown field data type was given, fall back to the default decoder.
     */
    public RedisFieldDecoder<?> getFieldDecoder(String rowDataFormat, Class<?> fieldType, @Nullable String fieldDataFormat)
    {
        checkNotNull(rowDataFormat, "rowDataFormat is null");
        checkNotNull(fieldType, "fieldType is null");

        checkState(fieldDecoders.containsKey(rowDataFormat), "no field decoders for '%s' found", rowDataFormat);
        Set<RedisFieldDecoder<?>> decoders = fieldDecoders.get(rowDataFormat).get(fieldType);

        ImmutableSet<String> fieldNames = ImmutableSet.of(
                firstNonNull(fieldDataFormat, DEFAULT_FIELD_DECODER_NAME),
                DEFAULT_FIELD_DECODER_NAME);

        for (String fieldName : fieldNames) {
            for (RedisFieldDecoder<?> decoder : decoders) {
                if (fieldName.equals(decoder.getFieldDecoderName())) {
                    return decoder;
                }
            }
        }

        throw new IllegalStateException(format("No field decoder for %s/%s found!", rowDataFormat, fieldType));
    }
}
