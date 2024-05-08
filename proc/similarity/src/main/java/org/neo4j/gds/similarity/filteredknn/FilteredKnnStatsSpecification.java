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
package org.neo4j.gds.similarity.filteredknn;

import org.neo4j.gds.NullComputationResultConsumer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.procedures.algorithms.configuration.NewConfigFunction;
import org.neo4j.gds.procedures.algorithms.similarity.KnnStatsResult;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STATS;

@GdsCallable(
    name = "gds.alpha.knn.filtered.stats",
    aliases = {"gds.knn.filtered.stats"},
    description = FilteredKnnConstants.PROCEDURE_DESCRIPTION,
    executionMode = STATS
)
public class FilteredKnnStatsSpecification implements AlgorithmSpec<FilteredKnn, FilteredKnnResult, FilteredKnnStatsConfig, Stream<KnnStatsResult>, FilteredKnnFactory<FilteredKnnStatsConfig>> {
    @Override
    public String name() {
        return "FilteredKnnStats";
    }

    @Override
    public FilteredKnnFactory<FilteredKnnStatsConfig> algorithmFactory(ExecutionContext executionContext) {
        return new FilteredKnnFactory<>();
    }

    @Override
    public NewConfigFunction<FilteredKnnStatsConfig> newConfigFunction() {
        return (__, userInput) -> FilteredKnnStatsConfig.of(userInput);
    }

    @Override
    public ComputationResultConsumer<FilteredKnn, FilteredKnnResult, FilteredKnnStatsConfig, Stream<KnnStatsResult>> computationResultConsumer() {
        return new NullComputationResultConsumer<>();
    }
}
