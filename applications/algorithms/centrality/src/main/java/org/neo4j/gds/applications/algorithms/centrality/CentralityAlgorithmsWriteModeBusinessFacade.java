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
package org.neo4j.gds.applications.algorithms.centrality;

import org.neo4j.gds.algorithms.centrality.CentralityAlgorithmResult;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.machinery.WriteContext;
import org.neo4j.gds.applications.algorithms.machinery.WriteNodePropertyService;
import org.neo4j.gds.applications.algorithms.machinery.WriteToDatabase;
import org.neo4j.gds.applications.algorithms.metadata.NodePropertiesWritten;
import org.neo4j.gds.betweenness.BetweennessCentralityWriteConfig;
import org.neo4j.gds.closeness.ClosenessCentralityWriteConfig;
import org.neo4j.gds.degree.DegreeCentralityWriteConfig;
import org.neo4j.gds.harmonic.HarmonicCentralityWriteConfig;
import org.neo4j.gds.harmonic.HarmonicResult;
import org.neo4j.gds.influenceMaximization.CELFResult;
import org.neo4j.gds.influenceMaximization.InfluenceMaximizationWriteConfig;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.pagerank.PageRankResult;
import org.neo4j.gds.pagerank.PageRankWriteConfig;

import java.util.Optional;

import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.ArticleRank;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.BetweennessCentrality;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.CELF;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.ClosenessCentrality;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.DegreeCentrality;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.EigenVector;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.HarmonicCentrality;
import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.PageRank;

public final class CentralityAlgorithmsWriteModeBusinessFacade {
    private final CentralityAlgorithmsEstimationModeBusinessFacade estimationFacade;
    private final CentralityAlgorithms centralityAlgorithms;
    private final AlgorithmProcessingTemplate algorithmProcessingTemplate;
    private final WriteToDatabase writeToDatabase;

    private CentralityAlgorithmsWriteModeBusinessFacade(
        CentralityAlgorithmsEstimationModeBusinessFacade estimationFacade,
        CentralityAlgorithms centralityAlgorithms,
        AlgorithmProcessingTemplate algorithmProcessingTemplate,
        WriteToDatabase writeToDatabase
    ) {
        this.estimationFacade = estimationFacade;
        this.centralityAlgorithms = centralityAlgorithms;
        this.algorithmProcessingTemplate = algorithmProcessingTemplate;
        this.writeToDatabase = writeToDatabase;
    }

    public static CentralityAlgorithmsWriteModeBusinessFacade create(
        Log log,
        RequestScopedDependencies requestScopedDependencies,
        WriteContext writeContext,
        CentralityAlgorithmsEstimationModeBusinessFacade estimationFacade,
        CentralityAlgorithms centralityAlgorithms,
        AlgorithmProcessingTemplate algorithmProcessingTemplate
    ) {
        var writeNodePropertyService = new WriteNodePropertyService(log, requestScopedDependencies, writeContext);
        var writeToDatabase = new WriteToDatabase(writeNodePropertyService);

        return new CentralityAlgorithmsWriteModeBusinessFacade(
            estimationFacade,
            centralityAlgorithms,
            algorithmProcessingTemplate,
            writeToDatabase
        );
    }

    public <RESULT> RESULT articleRank(
        GraphName graphName,
        PageRankWriteConfig configuration,
        ResultBuilder<PageRankWriteConfig, PageRankResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new PageRankWriteStep(writeToDatabase, configuration, ArticleRank);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            ArticleRank,
            estimationFacade::pageRank,
            graph -> centralityAlgorithms.articleRank(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <RESULT> RESULT betweennessCentrality(
        GraphName graphName,
        BetweennessCentralityWriteConfig configuration,
        ResultBuilder<BetweennessCentralityWriteConfig, CentralityAlgorithmResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new BetweennessCentralityWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            BetweennessCentrality,
            () -> estimationFacade.betweennessCentrality(configuration),
            graph -> centralityAlgorithms.betweennessCentrality(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <CONFIGURATION extends InfluenceMaximizationWriteConfig, RESULT> RESULT celf(
        GraphName graphName,
        CONFIGURATION configuration,
        ResultBuilder<CONFIGURATION, CELFResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new CelfWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            CELF,
            () -> estimationFacade.celf(configuration),
            graph -> centralityAlgorithms.celf(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <RESULT> RESULT closenessCentrality(
        GraphName graphName,
        ClosenessCentralityWriteConfig configuration,
        ResultBuilder<ClosenessCentralityWriteConfig, CentralityAlgorithmResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new ClosenessCentralityWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            ClosenessCentrality,
            () -> estimationFacade.closenessCentrality(configuration),
            graph -> centralityAlgorithms.closenessCentrality(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <RESULT> RESULT degreeCentrality(
        GraphName graphName,
        DegreeCentralityWriteConfig configuration,
        ResultBuilder<DegreeCentralityWriteConfig, CentralityAlgorithmResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new DegreeCentralityWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            DegreeCentrality,
            () -> estimationFacade.degreeCentrality(configuration),
            graph -> centralityAlgorithms.degreeCentrality(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <RESULT> RESULT eigenvector(
        GraphName graphName,
        PageRankWriteConfig configuration,
        ResultBuilder<PageRankWriteConfig, PageRankResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new PageRankWriteStep(writeToDatabase, configuration, EigenVector);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            EigenVector,
            estimationFacade::pageRank,
            graph -> centralityAlgorithms.eigenVector(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <CONFIGURATION extends HarmonicCentralityWriteConfig, RESULT> RESULT harmonicCentrality(
        GraphName graphName,
        CONFIGURATION configuration,
        ResultBuilder<CONFIGURATION, HarmonicResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new HarmonicCentralityWriteStep(writeToDatabase, configuration);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            HarmonicCentrality,
            estimationFacade::harmonicCentrality,
            graph -> centralityAlgorithms.harmonicCentrality(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }

    public <RESULT> RESULT pageRank(
        GraphName graphName,
        PageRankWriteConfig configuration,
        ResultBuilder<PageRankWriteConfig, PageRankResult, RESULT, NodePropertiesWritten> resultBuilder
    ) {
        var writeStep = new PageRankWriteStep(writeToDatabase, configuration, ArticleRank);

        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            PageRank,
            estimationFacade::pageRank,
            graph -> centralityAlgorithms.pageRank(graph, configuration),
            Optional.of(writeStep),
            resultBuilder
        );
    }
}
