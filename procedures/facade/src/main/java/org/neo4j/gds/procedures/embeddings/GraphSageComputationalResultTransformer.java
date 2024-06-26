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
package org.neo4j.gds.procedures.embeddings;

import org.neo4j.gds.algorithms.StreamComputationResult;
import org.neo4j.gds.algorithms.TrainResult;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.core.model.Model;
import org.neo4j.gds.embeddings.graphsage.GraphSageModelTrainer;
import org.neo4j.gds.embeddings.graphsage.ModelData;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageResult;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainConfig;
import org.neo4j.gds.procedures.embeddings.graphsage.GraphSageStreamResult;
import org.neo4j.gds.procedures.embeddings.graphsage.GraphSageTrainResult;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class GraphSageComputationalResultTransformer {

    public static Stream<GraphSageStreamResult> toStreamResult(
        StreamComputationResult<GraphSageResult> computationResult
    ) {
        return computationResult.result().map(graphSageResult -> {
            var graph = computationResult.graph();
            var embeddings = graphSageResult.embeddings();
            return LongStream.range(IdMap.START_NODE_ID, graph.nodeCount())
                .mapToObj(internalNodeId -> new GraphSageStreamResult(
                    graph.toOriginalNodeId(internalNodeId),
                    embeddings.get(internalNodeId)
                ));

        }).orElseGet(Stream::empty);
    }

    public static GraphSageTrainResult toTrainResult(
        TrainResult<Model<ModelData, GraphSageTrainConfig, GraphSageModelTrainer.GraphSageTrainMetrics>> trainResult
    ) {

        return new GraphSageTrainResult(trainResult.algorithmSpecificFields(), trainResult.trainMillis());
    }
}
