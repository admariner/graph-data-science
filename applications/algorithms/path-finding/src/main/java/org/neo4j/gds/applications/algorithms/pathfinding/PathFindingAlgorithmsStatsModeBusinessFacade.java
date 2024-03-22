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
package org.neo4j.gds.applications.algorithms.pathfinding;

import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.paths.traverse.BfsStatsConfig;
import org.neo4j.gds.steiner.SteinerTreeResult;
import org.neo4j.gds.steiner.SteinerTreeStatsConfig;

import java.util.Optional;

import static org.neo4j.gds.applications.algorithms.pathfinding.AlgorithmLabels.BFS;
import static org.neo4j.gds.applications.algorithms.pathfinding.AlgorithmLabels.STEINER;

public class PathFindingAlgorithmsStatsModeBusinessFacade {
    private final AlgorithmProcessingTemplate algorithmProcessingTemplate;

    private final PathFindingAlgorithmsEstimationModeBusinessFacade estimationFacade;
    private final PathFindingAlgorithms pathFindingAlgorithms;

    public PathFindingAlgorithmsStatsModeBusinessFacade(
        AlgorithmProcessingTemplate algorithmProcessingTemplate,
        PathFindingAlgorithmsEstimationModeBusinessFacade estimationFacade,
        PathFindingAlgorithms pathFindingAlgorithms
    ) {
        this.algorithmProcessingTemplate = algorithmProcessingTemplate;
        this.estimationFacade = estimationFacade;
        this.pathFindingAlgorithms = pathFindingAlgorithms;
    }

    public <RESULT> RESULT breadthFirstSearch(
        GraphName graphName,
        BfsStatsConfig configuration,
        ResultBuilder<BfsStatsConfig, HugeLongArray, RESULT> resultBuilder
    ) {
        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            BFS,
            () -> estimationFacade.breadthFirstSearchEstimation(configuration),
            graph -> pathFindingAlgorithms.breadthFirstSearch(graph, configuration),
            Optional.empty(),
            resultBuilder
        );
    }

    public <RESULT> RESULT steinerTree(
        GraphName graphName,
        SteinerTreeStatsConfig configuration,
        ResultBuilder<SteinerTreeStatsConfig, SteinerTreeResult, RESULT> resultBuilder
    ) {
        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            STEINER,
            () -> estimationFacade.steinerTreeEstimation(configuration),
            graph -> pathFindingAlgorithms.steinerTree(graph, configuration),
            Optional.empty(),
            resultBuilder
        );
    }
}