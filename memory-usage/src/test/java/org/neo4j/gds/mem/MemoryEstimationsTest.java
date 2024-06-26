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
package org.neo4j.gds.mem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.ImmutableGraphDimensions;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryEstimationsTest {
    private static final GraphDimensions DIMENSIONS_100_NODES = ImmutableGraphDimensions.builder().nodeCount(100).build();

    @Test
    void testEmptyBuilder() {
        MemoryEstimation empty = MemoryEstimations.empty();
        assertEquals("", empty.description());
        assertTrue(empty.components().isEmpty());
        Assertions.assertEquals(
                MemoryRange.empty(),
                empty.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testBuilderForClass() {
        MemoryEstimation memoryEstimation1 = MemoryEstimations.builder(Foo.class).build();
        MemoryEstimation memoryEstimation2 = MemoryEstimations
                .builder()
                .field(Foo.class.getSimpleName(), Foo.class)
                .build();
        MemoryEstimation memoryEstimation3 = MemoryEstimations
                .builder()
                .startField(Foo.class.getSimpleName(), Foo.class)
                .endField()
                .build();
        MemoryEstimation memoryEstimation4 = MemoryEstimations.of(Foo.class);
        var concurrency = new Concurrency(4);
        assertEquals(MemoryRange.of(40L), memoryEstimation1.estimate(DIMENSIONS_100_NODES, concurrency).memoryUsage());
        assertEquals(MemoryRange.of(40L), memoryEstimation2.estimate(DIMENSIONS_100_NODES, concurrency).memoryUsage());
        assertEquals(MemoryRange.of(40L), memoryEstimation3.estimate(DIMENSIONS_100_NODES, concurrency).memoryUsage());
        assertEquals(MemoryRange.of(40L), memoryEstimation4.estimate(DIMENSIONS_100_NODES, concurrency).memoryUsage());
    }

    @Test
    void testFixed() {
        MemoryEstimation memoryEstimation = MemoryEstimations.builder()
                .fixed("bar", 23L)
                .fixed("baz", MemoryRange.of(19))
                .build();

        assertEquals(MemoryRange.of(42L), memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testAdd() {
        MemoryEstimation memoryEstimation = MemoryEstimations.builder()
                .add("foo", MemoryEstimations.of(Foo.class))
                .add(MemoryEstimations.of(Foo.class))
                .build();

        assertEquals(MemoryRange.of(80L), memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testPerNode() {
        MemoryEstimation memoryEstimation = MemoryEstimations.builder()
                .perNode("foo", nodeCount -> nodeCount * 42)
                .perNode("bar", MemoryEstimations.of(Foo.class))
                .rangePerNode("baz", nodeCount -> MemoryRange.of(23).times(nodeCount))
                .build();

        assertEquals(
                MemoryRange.of(100 * (42 + 40 + 23)),
                memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testPerThread() {
        MemoryEstimation memoryEstimation = MemoryEstimations.builder()
                .perThread("foo", concurrency -> concurrency * 42)
                .perThread("bar", MemoryEstimations.of(Foo.class))
                .build();

        assertEquals(MemoryRange.of(4 * (42 + 40)), memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testPerGraphDimension() {
        MemoryEstimation memoryEstimation = MemoryEstimations.builder()
                .perGraphDimension("foo", (graphDimensions, concurrency) -> MemoryRange.of(graphDimensions.nodeCount() * 42))
                .rangePerGraphDimension("bar", (graphDimensions, concurrency) -> MemoryRange.of(23).times(graphDimensions.nodeCount()))
                .build();

        assertEquals(
                MemoryRange.of(100 * 42 + 100 * 23),
                memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testSetup() {
        final MemoryEstimation memoryEstimation = MemoryEstimations.setup(
                "foo",
                (dimensions, concurrency) -> MemoryEstimations.builder()
                        .fixed("foo", dimensions.nodeCount() * concurrency.value())
                        .build());

        assertEquals(
                MemoryRange.of(100 * 4),
                memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void testSetupFromGraphDimensions() {
        final MemoryEstimation memoryEstimation = MemoryEstimations.setup(
                "foo",
                (dimensions) -> MemoryEstimations.builder()
                        .fixed("foo", dimensions.nodeCount() * dimensions.nodeCount())
                        .build());

        assertEquals(
                MemoryRange.of(100 * 100),
                memoryEstimation.estimate(DIMENSIONS_100_NODES, new Concurrency(4)).memoryUsage());
    }

    @Test
    void shouldPickBiggestMemoryUser() {
        MemoryEstimation maxEstimator = MemoryEstimations.builder()
            .max(List.of(
                    MemoryEstimations.builder("node storer").perNode("node storer", nodeCount -> nodeCount).build(),
                    MemoryEstimations.builder("paralleliser").perThread("paralleliser", threadCount -> threadCount).build()
                )
            )
            .build();

        assertThat(biggestMemoryUser(maxEstimator.estimate(GraphDimensions.of(1000), new Concurrency(1)))).isEqualTo("node storer");
        assertThat(biggestMemoryUser(maxEstimator.estimate(GraphDimensions.of(1), new Concurrency(16)))).isEqualTo("paralleliser");
    }

    private String biggestMemoryUser(MemoryTree maxEstimate) {
        var iterator = maxEstimate.components().iterator().next().components().iterator();
        var c1 = iterator.next();
        var c2 = iterator.next();
        if (c1.memoryUsage().max < c2.memoryUsage().max) {
            return c2.description();
        } else {
            return c1.description();
        }
    }

    // Note that the memory consumption will be aligned (see BitUtil.align)
    static class Foo { // 12 Byte object header
        int bar; // 4 Byte
        long baz; // 8 Byte
        long[] lol; // 4 Byte
        String lulz; // 4 Byte
        boolean lolz; // 1 Byte
    }
}
