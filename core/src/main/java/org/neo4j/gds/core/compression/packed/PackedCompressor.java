/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.core.compression.packed;

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.PropertyMappings;
import org.neo4j.gds.api.AdjacencyList;
import org.neo4j.gds.api.AdjacencyProperties;
import org.neo4j.gds.api.compress.AdjacencyCompressor;
import org.neo4j.gds.api.compress.AdjacencyCompressorFactory;
import org.neo4j.gds.api.compress.AdjacencyListBuilder;
import org.neo4j.gds.api.compress.AdjacencyListBuilderFactory;
import org.neo4j.gds.api.compress.LongArrayBuffer;
import org.neo4j.gds.api.compress.ModifiableSlice;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.compression.common.AbstractAdjacencyCompressorFactory;
import org.neo4j.gds.core.compression.common.AdjacencyCompression;
import org.neo4j.gds.core.utils.paged.HugeIntArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.Arrays;
import java.util.function.LongSupplier;

public final class PackedCompressor implements AdjacencyCompressor {

    public static AdjacencyCompressorFactory factory(
        LongSupplier nodeCountSupplier,
        AdjacencyListBuilderFactory<Long, ? extends AdjacencyList, long[], ? extends AdjacencyProperties> adjacencyListBuilderFactory,
        PropertyMappings propertyMappings,
        Aggregation[] aggregations,
        boolean noAggregation
    ) {
        AdjacencyListBuilder<long[], ? extends AdjacencyProperties>[] propertyBuilders = new AdjacencyListBuilder[propertyMappings.numberOfMappings()];
        Arrays.setAll(propertyBuilders, i -> adjacencyListBuilderFactory.newAdjacencyPropertiesBuilder());

        return new Factory(
            nodeCountSupplier,
            adjacencyListBuilderFactory.newAdjacencyListBuilder(),
            propertyBuilders,
            noAggregation,
            aggregations
        );
    }

    static class Factory extends AbstractAdjacencyCompressorFactory<Long, long[]> {

        Factory(
            LongSupplier nodeCountSupplier,
            AdjacencyListBuilder<Long, ? extends AdjacencyList> adjacencyBuilder,
            AdjacencyListBuilder<long[], ? extends AdjacencyProperties>[] propertyBuilders,
            boolean noAggregation,
            Aggregation[] aggregations
        ) {
            super(
                nodeCountSupplier,
                adjacencyBuilder,
                propertyBuilders,
                noAggregation,
                aggregations
            );
        }

        @Override
        protected AdjacencyCompressor createCompressorFromInternalState(
            AdjacencyListBuilder<Long, ? extends AdjacencyList> adjacencyBuilder,
            AdjacencyListBuilder<long[], ? extends AdjacencyProperties>[] propertyBuilders,
            boolean noAggregation,
            Aggregation[] aggregations,
            HugeIntArray adjacencyDegrees,
            HugeLongArray adjacencyOffsets,
            HugeLongArray propertyOffsets
        ) {
            AdjacencyListBuilder.Allocator<long[]> firstAllocator;
            AdjacencyListBuilder.PositionalAllocator<long[]>[] otherAllocators;

            if (propertyBuilders.length > 0) {
                firstAllocator = propertyBuilders[0].newAllocator();
                //noinspection unchecked
                otherAllocators = new AdjacencyListBuilder.PositionalAllocator[propertyBuilders.length - 1];
                Arrays.setAll(
                    otherAllocators,
                    i -> propertyBuilders[i + 1].newPositionalAllocator()
                );
            } else {
                firstAllocator = null;
                otherAllocators = null;
            }

            return new PackedCompressor(
                adjacencyBuilder.newAllocator(),
                firstAllocator,
                otherAllocators,
                adjacencyDegrees,
                adjacencyOffsets,
                propertyOffsets,
                noAggregation,
                aggregations
            );
        }

//        private final LongSupplier nodeCountSupplier;
//        private final PropertyMappings propertyMappings;
//        private final Aggregation[] aggregations;
//        private final boolean noAggregation;
//
//        private final LongAdder relationshipCounter;
//
//        private HugeObjectArray<Compressed> adjacencies;
//
//        Factory(
//            LongSupplier nodeCountSupplier,
//            PropertyMappings propertyMappings,
//            Aggregation[] aggregations,
//            boolean noAggregation
//        ) {
//            this.nodeCountSupplier = nodeCountSupplier;
//            this.propertyMappings = propertyMappings;
//            this.aggregations = aggregations;
//            this.noAggregation = noAggregation;
//            this.relationshipCounter = new LongAdder();
//        }
//
//        @Override
//        public void init() {
//            long nodeCount = this.nodeCountSupplier.getAsLong();
//            this.adjacencies = HugeObjectArray.newArray(Compressed.class, nodeCount);
//        }
//
//        @Override
//        public AdjacencyCompressor createCompressor() {
//            return new PackedCompressor(this.adjacencies, this.aggregations, this.noAggregation);
//        }
//
//        @Override
//        public LongAdder relationshipCounter() {
//            return this.relationshipCounter;
//        }
//
//        @Override
//        public AdjacencyListsWithProperties build() {
//            var adjacency = new PackedAdjacencyList(this.adjacencies);
//
//            var builder = ImmutableAdjacencyListsWithProperties.builder()
//                .adjacency(adjacency)
//                .relationshipCount(this.relationshipCounter.longValue());
//
//            var mappings = this.propertyMappings.mappings();
//            for (int i = 0; i < mappings.size(); i++) {
//                var property = new PackedPropertyList(this.adjacencies, i);
//                builder.addProperty(property);
//            }
//
//            return builder.build();
//        }
    }

    static final int FLAGS = AdjacencyPacker.DELTA | AdjacencyPacker.SORT;

    private final AdjacencyListBuilder.Allocator<Long> adjacencyAllocator;
    private final @Nullable AdjacencyListBuilder.Allocator<long[]> firstPropertyAllocator;
    private final AdjacencyListBuilder.PositionalAllocator<long[]> @Nullable [] otherPropertyAllocators;
    private final HugeIntArray adjacencyDegrees;
    private final HugeLongArray adjacencyOffsets;
    private final HugeLongArray propertyOffsets;
    private final boolean noAggregation;
    private final Aggregation[] aggregations;

    private final LongArrayBuffer buffer;
    private final ModifiableSlice<Long> adjacencySlice;
    private final ModifiableSlice<long[]> propertySlice;

    // TODO: only used for non-property case
    private final int flags;

    private PackedCompressor(
        AdjacencyListBuilder.Allocator<Long> adjacencyAllocator,
        @Nullable AdjacencyListBuilder.Allocator<long[]> firstPropertyAllocator,
        AdjacencyListBuilder.PositionalAllocator<long[]> @Nullable [] otherPropertyAllocators,
        HugeIntArray adjacencyDegrees,
        HugeLongArray adjacencyOffsets,
        HugeLongArray propertyOffsets,
        boolean noAggregation,
        Aggregation[] aggregations
    ) {
        this.adjacencyAllocator = adjacencyAllocator;
        this.firstPropertyAllocator = firstPropertyAllocator;
        this.otherPropertyAllocators = otherPropertyAllocators;
        this.adjacencyDegrees = adjacencyDegrees;
        this.adjacencyOffsets = adjacencyOffsets;
        this.propertyOffsets = propertyOffsets;
        this.noAggregation = noAggregation;
        this.aggregations = aggregations;

        // TODO: only used for non-property case
        this.flags = FLAGS | aggregations[0].ordinal();

        this.buffer = new LongArrayBuffer();
        this.adjacencySlice = ModifiableSlice.create();
        this.propertySlice = ModifiableSlice.create();
    }

    @Override
    public int compress(
        long nodeId,
        byte[] targets,
        long[][] properties,
        int numberOfCompressedTargets,
        int compressedBytesSize,
        ValueMapper mapper
    ) {
        if (properties != null) {
            return packWithProperties(
                nodeId,
                targets,
                properties,
                numberOfCompressedTargets,
                compressedBytesSize,
                mapper
            );
        } else {
            return packWithoutProperties(
                nodeId,
                targets,
                numberOfCompressedTargets,
                compressedBytesSize,
                mapper
            );
        }
    }

    private int packWithProperties(
        long nodeId,
        byte[] semiCompressedBytesDuringLoading,
        long[][] uncompressedPropertiesPerProperty,
        int numberOfCompressedTargets,
        int compressedByteSize,
        ValueMapper mapper
    ) {
        AdjacencyCompression.zigZagUncompressFrom(
            this.buffer,
            semiCompressedBytesDuringLoading,
            numberOfCompressedTargets,
            compressedByteSize,
            mapper
        );

        long[] targets = this.buffer.buffer;
        int targetsLength = this.buffer.length;

        long offset = AdjacencyPacker2.compressWithProperties(
            this.adjacencyAllocator,
            this.adjacencySlice,
            targets,
            uncompressedPropertiesPerProperty,
            targetsLength,
            this.aggregations,
            this.noAggregation
        );

        int degree = this.adjacencySlice.length();

        copyProperties(uncompressedPropertiesPerProperty, degree, nodeId);

        this.adjacencyDegrees.set(nodeId, degree);
        this.adjacencyOffsets.set(nodeId, offset);

        return degree;
    }

    private int packWithoutProperties(
        long nodeId,
        byte[] semiCompressedBytesDuringLoading,
        int numberOfCompressedTargets,
        int compressedByteSize,
        ValueMapper mapper
    ) {
        AdjacencyCompression.zigZagUncompressFrom(
            this.buffer,
            semiCompressedBytesDuringLoading,
            numberOfCompressedTargets,
            compressedByteSize,
            mapper
        );

        long[] targets = this.buffer.buffer;
        int targetsLength = this.buffer.length;

        long offset = AdjacencyPacker2.compress(
            this.adjacencyAllocator,
            this.adjacencySlice,
            targets,
            targetsLength,
            this.aggregations[0]
        );
        int degree = this.adjacencySlice.length();

        this.adjacencyOffsets.set(nodeId, offset);
        this.adjacencyDegrees.set(nodeId, degree);

        return degree;
    }

    private void copyProperties(long[][] properties, int degree, long nodeId) {
        assert this.firstPropertyAllocator != null;
        assert this.otherPropertyAllocators != null;

        var slice = this.propertySlice;
        long address = this.firstPropertyAllocator.allocate(degree, slice);
        System.arraycopy(properties[0], 0, slice.slice(), slice.offset(), degree);

        for (int i = 1; i < properties.length; i++) {
            this.otherPropertyAllocators[i - 1].writeAt(address, properties[i], degree);
        }

        this.propertyOffsets.set(nodeId, address);
    }

    @Override
    public void close() {

    }
}
