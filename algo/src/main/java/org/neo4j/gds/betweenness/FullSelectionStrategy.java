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
package org.neo4j.gds.betweenness;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class FullSelectionStrategy implements SelectionStrategy {

    private final AtomicLong nodeQueue = new AtomicLong();
    private long graphSize = 0;

    @Override
    public void init(Graph graph, ExecutorService ignored, Concurrency unused) {
        this.graphSize = graph.nodeCount();
        nodeQueue.set(0);
    }

    @Override
    public long next() {
        long nextNodeId = nodeQueue.getAndIncrement();
        if (nextNodeId >= graphSize) {
            return NONE_SELECTED;
        }
        return nextNodeId;
    }
}
