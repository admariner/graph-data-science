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
package org.neo4j.gds.procedures.algorithms.pathfinding;

import org.neo4j.gds.api.CloseableResourceRegistry;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.NodeLookup;
import org.neo4j.gds.applications.algorithms.machinery.StreamResultBuilder;
import org.neo4j.gds.paths.bellmanford.BellmanFordResult;
import org.neo4j.gds.paths.dijkstra.PathFindingResult;

import java.util.Optional;
import java.util.stream.Stream;

class BellmanFordResultBuilderForStreamMode implements StreamResultBuilder<BellmanFordResult, BellmanFordStreamResult> {
    private final CloseableResourceRegistry closeableResourceRegistry;
    private final NodeLookup nodeLookup;
    private final boolean routeRequested;

    BellmanFordResultBuilderForStreamMode(
        CloseableResourceRegistry closeableResourceRegistry,
        NodeLookup nodeLookup,
        boolean routeRequested
    ) {
        this.closeableResourceRegistry = closeableResourceRegistry;
        this.nodeLookup = nodeLookup;
        this.routeRequested = routeRequested;
    }

    @Override
    public Stream<BellmanFordStreamResult> build(
        Graph graph,
        GraphStore graphStore,
        Optional<BellmanFordResult> result
    ) {
        if (result.isEmpty()) return Stream.of();

        var bellmanFordResult = result.get();

        // this is us handling the case of generated graphs and such
        var shouldCreateRoutes = routeRequested && graphStore.capabilities().canWriteToLocalDatabase();

        var containsNegativeCycle = bellmanFordResult.containsNegativeCycle();

        var resultBuilder = new BellmanFordStreamResult.Builder(graph, nodeLookup)
            .withIsCycle(containsNegativeCycle);

        var algorithmResult = getPathFindingResult(bellmanFordResult, containsNegativeCycle);

        var resultStream = algorithmResult.mapPaths(path -> resultBuilder.build(
            path.nodeIds(),
            path.costs(),
            path.index(),
            path.sourceNode(),
            path.targetNode(),
            path.totalCost(),
            shouldCreateRoutes
        ));

        closeableResourceRegistry.register(resultStream);

        return resultStream;
    }

    private static PathFindingResult getPathFindingResult(
        BellmanFordResult result,
        boolean containsNegativeCycle
    ) {
        if (containsNegativeCycle) return result.negativeCycles();

        return result.shortestPaths();
    }
}
