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
package org.neo4j.gds.procedures.algorithms.community;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.StatsResultBuilder;
import org.neo4j.gds.modularity.ModularityResult;
import org.neo4j.gds.modularity.ModularityStatsConfig;

import java.util.Optional;
import java.util.stream.Stream;

class ModularityResultBuilderForStatsMode implements StatsResultBuilder<ModularityResult, Stream<ModularityStatsResult>> {
    private final ModularityStatsConfig configuration;

    ModularityResultBuilderForStatsMode(ModularityStatsConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public Stream<ModularityStatsResult> build(
        Graph graph,
        Optional<ModularityResult> result,
        AlgorithmProcessingTimings timings
    ) {
        if (result.isEmpty()) return Stream.of(ModularityStatsResult.emptyFrom(timings, configuration.toMap()));

        var modularityResult = result.get();

        var modularityStatsResult = new ModularityStatsResult(
            modularityResult.nodeCount(),
            modularityResult.relationshipCount(),
            modularityResult.communityCount(),
            modularityResult.totalModularity(),
            timings.preProcessingMillis,
            timings.computeMillis,
            0,
            configuration.toMap()
        );

        return Stream.of(modularityStatsResult);
    }
}
