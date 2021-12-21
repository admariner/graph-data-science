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
package org.neo4j.gds;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.AlgoBaseProcTest.InvocationCountingTaskStore;
import org.neo4j.gds.api.nodeproperties.LongNodeProperties;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.utils.mem.AllocationTracker;
import org.neo4j.gds.core.utils.progress.TaskRegistry;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.write.ImmutableNodeProperty;
import org.neo4j.gds.core.write.NativeNodePropertyExporter;
import org.neo4j.gds.gdl.GdlFactory;
import org.neo4j.gds.gdl.GdlGraphs;
import org.neo4j.gds.gdl.ImmutableGraphCreateFromGdlConfig;
import org.neo4j.gds.pipeline.ImmutableComputationResult;
import org.neo4j.gds.pipeline.ImmutableExecutionContext;
import org.neo4j.gds.test.TestAlgoResultBuilder;
import org.neo4j.gds.test.TestAlgorithm;
import org.neo4j.gds.test.TestResult;
import org.neo4j.gds.test.TestWriteConfig;
import org.neo4j.gds.transaction.TransactionContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WriteProcCancellationTest extends BaseTest {

    InvocationCountingTaskStore taskStore = new InvocationCountingTaskStore();

    @Test
    void shouldRemoveTaskAfterWriteFailure() {
        var nodeProperties = new LongNodeProperties() {
            @Override
            public long longValue(long nodeId) {
                throw new IllegalStateException("explicit fail while writing");
            }

            @Override
            public long size() {
                return 42;
            }
        };
        var nodeProperty = ImmutableNodeProperty.of("prop", nodeProperties);

        try (var tx = db.beginTx()) {

            var resultConsumer = new WriteNodePropertiesComputationResultConsumer<TestAlgorithm, TestAlgorithm, TestWriteConfig, TestResult>(
                (computationResult, executionContext) -> new TestAlgoResultBuilder(),
                (computationResult, allocationTracker) -> List.of(nodeProperty),
                new NativeNodePropertyExporter.Builder(TransactionContext.of(db, tx)),
                "foo"
            );

            var algorithm = new TestAlgorithm(
                GdlGraphs.EMPTY,
                AllocationTracker.empty(),
                42,
                ProgressTracker.NULL_TRACKER,
                false
            );

            var graphStore = GdlFactory
                .builder()
                .namedDatabaseId(db.databaseId())
                .createConfig(ImmutableGraphCreateFromGdlConfig.builder().graphName("foo").gdlGraph("(a)").build())
                .build()
                .build()
                .graphStore();

            var computationResult = ImmutableComputationResult.<TestAlgorithm, TestAlgorithm, TestWriteConfig>builder()
                .graphStore(graphStore)
                .graph(graphStore.getUnion())
                .config(TestWriteConfig.of(CypherMapWrapper.create(Map.of("writeProperty", "writeProp"))))
                .algorithm(algorithm)
                .result(algorithm)
                .computeMillis(0)
                .preProcessingMillis(0)
                .build();

            var executionContext = ImmutableExecutionContext
                .builder()
                .api(db)
                .taskRegistryFactory(() -> new TaskRegistry("", taskStore))
                .username("")
                .allocationTracker(AllocationTracker.empty())
                .log(new TestLog())
                .build();

            assertThatThrownBy(() -> resultConsumer.consume(computationResult, executionContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("explicit fail while writing");
            assertThat(taskStore.removeTaskInvocations).isGreaterThanOrEqualTo(1);
            assertThat(taskStore.taskStream()).isEmpty();
        }
    }
}
