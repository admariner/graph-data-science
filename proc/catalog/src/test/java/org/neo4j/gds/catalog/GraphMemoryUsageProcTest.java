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
package org.neo4j.gds.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.beta.generator.GraphGenerateProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.utils.GdsFeatureToggles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;

class GraphMemoryUsageProcTest extends BaseProcTest {

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
            GraphMemoryUsageProc.class,
            GraphGenerateProc.class
        );
    }

    @AfterEach
    void tearDown() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @Test
    void testShowMemoryUsage() {
        var graphName = "g";
        var params = Map.of("name", (Object) graphName);
        runQuery(
            "CALL gds.graph.generate($name, 100, 2)",
            params
        );

        assertCypherResult("CALL gds.internal.graph.sizeOf($name)", params, List.of(
            Map.of(
                "graphName", graphName,
                "memoryUsage", instanceOf(String.class),
                "sizeInBytes", allOf(instanceOf(Long.class), greaterThan(0L)),
                "detailSizeInBytes", allOf(
                    instanceOf(Map.class),
                    hasKey("relationships"),
                    hasKey("total"),
                    hasKey("nodes"),
                    hasKey("adjacencyLists")
                ),
                "nodeCount", 100L,
                "relationshipCount", 200L
            )
        ));
    }

    @Test
    void testWithAdjacencyMemoryTracking() {
        var graphName = "g";
        var params = Map.of("name", (Object) graphName);
        GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.enableAndRun(() -> {
            runQuery(
                "CALL gds.graph.generate($name, 100, 2)",
                params
            );

            assertCypherResult("CALL gds.internal.graph.sizeOf($name)", params, List.of(
                Map.of(
                    "graphName", graphName,
                    "memoryUsage", instanceOf(String.class),
                    "sizeInBytes", allOf(instanceOf(Long.class), greaterThan(0L)),
                    "detailSizeInBytes", Map.of(
                        "relationships", instanceOf(Map.class),
                        "total", allOf(instanceOf(Long.class), greaterThan(0L)),
                        "nodes", instanceOf(Map.class),
                        "adjacencyLists", Map.of(
                            "REL", Map.of(
                                "pages",
                                allOf(instanceOf(Long.class), greaterThan(0L)),
                                "bytesTotal",
                                allOf(instanceOf(Long.class), greaterThan(0L)),
                                "bytesOnHeap",
                                allOf(instanceOf(Long.class), greaterThan(0L)),
                                "bytesOffHeap",
                                instanceOf(Long.class),
                                "pageSizes",
                                allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThan(0D))),
                                "heapAllocations",
                                allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThan(0D))),
                                "nativeAllocations",
                                instanceOf(Map.class),
                                "headerBits",
                                instanceOf(Map.class),
                                "headerAllocations",
                                instanceOf(Map.class)
                            ))
                    ),
                    "nodeCount", 100L,
                    "relationshipCount", 200L
                )
            ));
        });
    }

    @Test
    void testWithAdjacencyMemoryTrackingWithPacking() {
        var graphName = "g";
        var params = Map.of("name", (Object) graphName);
        GdsFeatureToggles.ENABLE_ADJACENCY_COMPRESSION_MEMORY_TRACKING.enableAndRun(() -> {
            GdsFeatureToggles.USE_PACKED_ADJACENCY_LIST.enableAndRun(() -> {

                runQuery(
                    "CALL gds.graph.generate($name, 100, 2)",
                    params
                );

                var r = new HashMap<>();
                r.put("pages", allOf(instanceOf(Long.class), greaterThan(0L)));
                r.put("bytesTotal", allOf(instanceOf(Long.class), greaterThan(0L)));
                r.put("bytesOnHeap", allOf(instanceOf(Long.class), greaterThan(0L)));
                r.put("bytesOffHeap", allOf(instanceOf(Long.class), greaterThanOrEqualTo(0L)));
                r.put("pageSizes", instanceOf(Map.class));
                r.put("heapAllocations", allOf(instanceOf(Map.class)));
                r.put("nativeAllocations", instanceOf(Map.class));
                r.put("headerBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("headerAllocations", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("blockCount", allOf(instanceOf(Long.class), greaterThan(0L)));
                r.put("blockLengths", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("meanBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("medianBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("stdDevBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("maxBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("minBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("indexOfMaxValue", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("indexOfMinValue", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("headTailDiffBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("bestMaxDiffBits", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));
                r.put("exceptions", allOf(instanceOf(Map.class), hasEntry(equalTo("mean"), greaterThanOrEqualTo(0D))));

                assertCypherResult("CALL gds.internal.graph.sizeOf($name)", params, List.of(
                    Map.of(
                        "graphName", graphName,
                        "memoryUsage", instanceOf(String.class),
                        "sizeInBytes", allOf(instanceOf(Long.class), greaterThan(0L)),
                        "detailSizeInBytes", Map.of(
                            "relationships", instanceOf(Map.class),
                            "total", allOf(instanceOf(Long.class), greaterThan(0L)),
                            "nodes", instanceOf(Map.class),
                            "adjacencyLists", Map.of("REL", r)),
                        "nodeCount", 100L,
                        "relationshipCount", 200L
                    )
                ));
            });
        });
    }
}
