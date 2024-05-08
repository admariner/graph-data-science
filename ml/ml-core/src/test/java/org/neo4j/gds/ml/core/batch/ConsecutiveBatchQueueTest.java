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
package org.neo4j.gds.ml.core.batch;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.termination.TerminatedException;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ConsecutiveBatchQueueTest {

    @Test
    void totalSize() {
        long totalSize = 100_001_001L;
        var batchQueue = BatchQueue.consecutive(totalSize);
        assertThat(batchQueue.totalSize()).isEqualTo(totalSize);
        var counter = new AtomicLong(0);
        batchQueue.parallelConsume(batch -> counter.getAndAdd(batch.size()), new Concurrency(4), TerminationFlag.RUNNING_TRUE);
        assertThat(counter.get()).isEqualTo(totalSize);
    }

    @Test
    void shouldHandleVeryLargeBatches() {
        var concurrency = new Concurrency(4);
        var batchQueue = BatchQueue.consecutive(5L * Integer.MAX_VALUE, 1, concurrency);
        assertThat(batchQueue.pop().orElseThrow().size()).isEqualTo(Integer.MAX_VALUE);
        for (int i = 0; i < concurrency.value(); i++) {
            assertThat(batchQueue.pop()).isPresent();
        }
        assertThat(batchQueue.pop()).isEmpty();
    }

    @Test
    void shouldRespectMinBatchSize() {
        var batchQueue = BatchQueue.consecutive(100, 100, new Concurrency(4));
        int actualSize = batchQueue.pop().orElseThrow().size();
        assertThat(actualSize).isEqualTo(100);
        assertThat(batchQueue.pop()).isEmpty();
    }

    @Test
    void shouldNotGiveBatchLargerThanNodeCount() {
        var batchQueue = BatchQueue.consecutive(100, 200, new Concurrency(4));
        int actualSize = batchQueue.pop().orElseThrow().size();
        assertThat(actualSize).isEqualTo(100);
        assertThat(batchQueue.pop()).isEmpty();
    }

    @Test
    void shouldDivideNodesAlmostEqually() {
        var batchQueue = BatchQueue.consecutive(101, 1, new Concurrency(4));
        for (int i = 0; i < 3; i++) {
            assertThat(batchQueue.pop().orElseThrow().size()).isEqualTo(26);
        }
        assertThat(batchQueue.pop().orElseThrow().size()).isEqualTo(23);
        assertThat(batchQueue.pop()).isEmpty();
    }

    @Test
    void checkTerminationFlag() {
        var concurrency = new Concurrency(4);
        TerminationFlag flag = () -> false;
        var batchQueue = BatchQueue.consecutive(101, 1, concurrency);

        assertThatThrownBy(() -> batchQueue.parallelConsume(Batch::elementIds, concurrency, flag))
            .isInstanceOf(TerminatedException.class)
            .hasMessageContaining("The execution has been terminated.");
    }
}
