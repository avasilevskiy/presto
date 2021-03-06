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
package com.facebook.presto.kafka;

import com.facebook.presto.utils.decoder.DecodableColumnHandle;
import com.facebook.presto.utils.decoder.DecoderRegistry;
import com.facebook.presto.utils.decoder.FieldDecoder;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.ConnectorRecordSetProvider;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.RecordSet;
import com.facebook.presto.utils.decoder.RowDecoder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory for Kafka specific {@link RecordSet} instances.
 */
public class KafkaRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private final KafkaHandleResolver handleResolver;
    private final KafkaSimpleConsumerManager consumerManager;
    private final DecoderRegistry registry;

    @Inject
    public KafkaRecordSetProvider(
            DecoderRegistry registry,
            KafkaHandleResolver handleResolver,
            KafkaSimpleConsumerManager consumerManager)
    {
        this.registry = checkNotNull(registry, "registry is null");
        this.handleResolver = checkNotNull(handleResolver, "handleResolver is null");
        this.consumerManager = checkNotNull(consumerManager, "consumerManager is null");
    }

    @Override
    public RecordSet getRecordSet(ConnectorSplit split, List<? extends ColumnHandle> columns)
    {
        KafkaSplit kafkaSplit = handleResolver.convertSplit(split);

        ImmutableList.Builder<DecodableColumnHandle> handleBuilder = ImmutableList.builder();
        ImmutableMap.Builder<DecodableColumnHandle, FieldDecoder<?>> keyFieldDecoderBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<DecodableColumnHandle, FieldDecoder<?>> messageFieldDecoderBuilder = ImmutableMap.builder();

        RowDecoder keyDecoder = registry.getRowDecoder(kafkaSplit.getKeyDataFormat());
        RowDecoder messageDecoder = registry.getRowDecoder(kafkaSplit.getMessageDataFormat());

        for (ColumnHandle handle : columns) {
            KafkaColumnHandle columnHandle = handleResolver.convertColumnHandle(handle);
            handleBuilder.add(columnHandle);

            if (!columnHandle.isInternal()) {
                if (columnHandle.isKeyDecoder()) {
                    FieldDecoder<?> fieldDecoder = registry.getFieldDecoder(
                            kafkaSplit.getKeyDataFormat(),
                            columnHandle.getType().getJavaType(),
                            columnHandle.getDataFormat());

                    keyFieldDecoderBuilder.put(columnHandle, fieldDecoder);
                }
                else {
                    FieldDecoder<?> fieldDecoder = registry.getFieldDecoder(
                            kafkaSplit.getMessageDataFormat(),
                            columnHandle.getType().getJavaType(),
                            columnHandle.getDataFormat());

                    messageFieldDecoderBuilder.put(columnHandle, fieldDecoder);
                }
            }
        }

        ImmutableList<DecodableColumnHandle> handles = handleBuilder.build();
        ImmutableMap<DecodableColumnHandle, FieldDecoder<?>> keyFieldDecoders = keyFieldDecoderBuilder.build();
        ImmutableMap<DecodableColumnHandle, FieldDecoder<?>> messageFieldDecoders = messageFieldDecoderBuilder.build();

        return new KafkaRecordSet(kafkaSplit, consumerManager, handles, keyDecoder, messageDecoder, keyFieldDecoders, messageFieldDecoders);
    }
}
