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
package org.neo4j.graphalgo.beta.pregel;

import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimation;
import org.neo4j.graphalgo.core.utils.mem.MemoryEstimations;

class AsyncQueueMessenger implements Messenger<PrimitiveAsyncDoubleQueues.Iterator> {

    private final PrimitiveAsyncDoubleQueues queues;

    AsyncQueueMessenger(long nodeCount, AllocationTracker tracker) {
        this.queues = PrimitiveAsyncDoubleQueues.of(nodeCount, tracker);
    }

    // TODO
    static MemoryEstimation memoryEstimation() {
        return MemoryEstimations.empty();
    }

    @Override
    public void initIteration(int iteration) {
        if (iteration > 0) {
            queues.compact();
        }
    }

    @Override
    public void sendTo(long targetNodeId, double message) {
        queues.push(targetNodeId, message);
    }

    @Override
    public PrimitiveAsyncDoubleQueues.Iterator messageIterator() {
        return new PrimitiveAsyncDoubleQueues.Iterator(queues);
    }

    @Override
    public void initMessageIterator(
        PrimitiveAsyncDoubleQueues.Iterator messageIterator,
        long nodeId,
        boolean isFirstIteration
    ) {
        messageIterator.init(nodeId);
    }

    @Override
    public void release() {
        queues.release();
    }

}
