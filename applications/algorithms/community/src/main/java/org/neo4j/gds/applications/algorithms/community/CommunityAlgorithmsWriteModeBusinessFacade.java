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
package org.neo4j.gds.applications.algorithms.community;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.machinery.WriteNodePropertyService;
import org.neo4j.gds.applications.algorithms.machinery.WriteToDatabase;
import org.neo4j.gds.applications.algorithms.metadata.NodePropertiesWritten;
import org.neo4j.gds.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.wcc.WccWriteConfig;

import java.util.Optional;

import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.WCC;

public final class CommunityAlgorithmsWriteModeBusinessFacade {
    private final CommunityAlgorithmsEstimationModeBusinessFacade estimationFacade;
    private final CommunityAlgorithms algorithms;
    private final AlgorithmProcessingTemplate algorithmProcessingTemplate;
    private final WriteToDatabase writeToDatabase;

    private CommunityAlgorithmsWriteModeBusinessFacade(
        CommunityAlgorithmsEstimationModeBusinessFacade estimationFacade,
        CommunityAlgorithms algorithms,
        AlgorithmProcessingTemplate algorithmProcessingTemplate,
        WriteToDatabase writeToDatabase
    ) {
        this.estimationFacade = estimationFacade;
        this.algorithms = algorithms;
        this.algorithmProcessingTemplate = algorithmProcessingTemplate;
        this.writeToDatabase = writeToDatabase;
    }

    public static CommunityAlgorithmsWriteModeBusinessFacade create(
        Log log,
        RequestScopedDependencies requestScopedDependencies,
        CommunityAlgorithmsEstimationModeBusinessFacade estimation,
        CommunityAlgorithms algorithms,
        AlgorithmProcessingTemplate algorithmProcessingTemplate
    ) {
        var writeNodePropertyService = new WriteNodePropertyService(log, requestScopedDependencies);
        var writeToDatabase = new WriteToDatabase(writeNodePropertyService);

        return new CommunityAlgorithmsWriteModeBusinessFacade(
            estimation,
            algorithms,
            algorithmProcessingTemplate,
            writeToDatabase
        );
    }

    public <RESULT> RESULT wcc(
        GraphName graphName,
        WccWriteConfig configuration,
        ResultBuilder<WccWriteConfig, DisjointSetStruct, RESULT, Pair<NodePropertiesWritten, NodePropertyValues>> resultBuilder
    ) {
        var writeStep = new WccWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            WCC,
            () -> estimationFacade.wcc(configuration),
            graph -> algorithms.wcc(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }
}
