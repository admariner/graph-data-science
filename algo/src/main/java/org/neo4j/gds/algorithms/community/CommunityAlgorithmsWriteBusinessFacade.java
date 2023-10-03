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
package org.neo4j.gds.algorithms.community;

import org.neo4j.gds.algorithms.AlgorithmComputationResult;
import org.neo4j.gds.algorithms.AlphaSccSpecificFields;
import org.neo4j.gds.algorithms.CommunityStatisticsSpecificFields;
import org.neo4j.gds.algorithms.KCoreSpecificFields;
import org.neo4j.gds.algorithms.NodePropertyWriteResult;
import org.neo4j.gds.algorithms.StandardCommunityStatisticsSpecificFields;
import org.neo4j.gds.api.DatabaseId;
import org.neo4j.gds.api.User;
import org.neo4j.gds.api.properties.nodes.NodePropertyValuesAdapter;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.WriteConfig;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.kcore.KCoreDecompositionWriteConfig;
import org.neo4j.gds.result.CommunityStatistics;
import org.neo4j.gds.result.StatisticsComputationInstructions;
import org.neo4j.gds.scc.SccAlphaWriteConfig;
import org.neo4j.gds.scc.SccWriteConfig;
import org.neo4j.gds.wcc.WccWriteConfig;

import java.util.Optional;
import java.util.function.Supplier;

public class CommunityAlgorithmsWriteBusinessFacade {

    private final CommunityAlgorithmsFacade communityAlgorithmsFacade;
    private final WriteNodePropertyService writeNodePropertyService;

    public CommunityAlgorithmsWriteBusinessFacade(
        CommunityAlgorithmsFacade communityAlgorithmsFacade,
        WriteNodePropertyService writeNodePropertyService
    ) {
        this.writeNodePropertyService = writeNodePropertyService;
        this.communityAlgorithmsFacade = communityAlgorithmsFacade;
    }

    public NodePropertyWriteResult<StandardCommunityStatisticsSpecificFields> wcc(
        String graphName,
        WccWriteConfig configuration,
        User user,
        DatabaseId databaseId,
        StatisticsComputationInstructions statisticsComputationInstructions
    ) {

        // 1. Run the algorithm and time the execution
        var intermediateResult = AlgorithmRunner.runWithTiming(
            () -> communityAlgorithmsFacade.wcc(graphName, configuration, user, databaseId)
        );
        var algorithmResult = intermediateResult.algorithmResult;

        return writeToDatabase(
            algorithmResult,
            configuration,
            (result, config) -> CommunityResultCompanion.nodePropertyValues(
                config.isIncremental(),
                config.seedProperty(),
                config.writeProperty(),
                config.consecutiveIds(),
                result.asNodeProperties(),
                config.minCommunitySize(),
                config.concurrency(),
                () -> algorithmResult.graphStore().nodeProperty(config.seedProperty())
            ),
            (result -> result::setIdOf),
            (result, componentCount, communitySummary) -> {
                return new StandardCommunityStatisticsSpecificFields(
                    componentCount,
                    communitySummary
                );
            },
            statisticsComputationInstructions,
            intermediateResult.computeMilliseconds,
            () -> StandardCommunityStatisticsSpecificFields.EMPTY,
            "WccWrite",
            configuration.writeConcurrency(),
            configuration.writeProperty(),
            configuration.arrowConnectionInfo()
        );

    }

    public NodePropertyWriteResult<KCoreSpecificFields> kcore(
        String graphName,
        KCoreDecompositionWriteConfig configuration,
        User user,
        DatabaseId databaseId
    ) {

        // 1. Run the algorithm and time the execution
        var intermediateResult = AlgorithmRunner.runWithTiming(
            () -> communityAlgorithmsFacade.kCore(graphName, configuration, user, databaseId)
        );
        var algorithmResult = intermediateResult.algorithmResult;

        return writeToDatabase(
            algorithmResult,
            configuration,
            (result, config) -> NodePropertyValuesAdapter.adapt(result.coreValues()),
            (result) -> new KCoreSpecificFields(result.degeneracy()),
            intermediateResult.computeMilliseconds,
            () -> KCoreSpecificFields.EMPTY,
            "KCoreWrite",
            configuration.writeConcurrency(),
            configuration.writeProperty(),
            configuration.arrowConnectionInfo()
        );

    }

    public NodePropertyWriteResult<StandardCommunityStatisticsSpecificFields> scc(
        String graphName,
        SccWriteConfig configuration,
        User user,
        DatabaseId databaseId,
        StatisticsComputationInstructions statisticsComputationInstructions
    ) {

        // 1. Run the algorithm and time the execution
        var intermediateResult = AlgorithmRunner.runWithTiming(
            () -> communityAlgorithmsFacade.scc(graphName, configuration, user, databaseId)
        );
        var algorithmResult = intermediateResult.algorithmResult;

        return writeToDatabase(
            algorithmResult,
            configuration,
            (result, config) -> CommunityResultCompanion.nodePropertyValues(
                config.consecutiveIds(),
                NodePropertyValuesAdapter.adapt(result),
                Optional.empty(),
                config.concurrency()
            ),
            (result -> result::get),
            (result, componentCount, communitySummary) -> {
                return new StandardCommunityStatisticsSpecificFields(
                    componentCount,
                    communitySummary
                );
            },
            statisticsComputationInstructions,
            intermediateResult.computeMilliseconds,
            () -> StandardCommunityStatisticsSpecificFields.EMPTY,
            "SccWrite",
            configuration.writeConcurrency(),
            configuration.writeProperty(),
            configuration.arrowConnectionInfo()
        );

    }

    public NodePropertyWriteResult<AlphaSccSpecificFields> alphaScc(
        String graphName,
        SccAlphaWriteConfig configuration,
        User user,
        DatabaseId databaseId,
        StatisticsComputationInstructions statisticsComputationInstructions
    ) {

        // 1. Run the algorithm and time the execution
        var intermediateResult = AlgorithmRunner.runWithTiming(
            () -> communityAlgorithmsFacade.scc(graphName, configuration, user, databaseId)
        );
        var algorithmResult = intermediateResult.algorithmResult;

        return writeToDatabase(
            algorithmResult,
            configuration,
            (result, config) -> NodePropertyValuesAdapter.adapt(result),
            (result -> result::get),
            (result, componentCount, communitySummary) -> {
                return new AlphaSccSpecificFields(
                    result.size(),
                    componentCount,
                    communitySummary
                );
            },
            statisticsComputationInstructions,
            intermediateResult.computeMilliseconds,
            () -> AlphaSccSpecificFields.EMPTY.EMPTY,
            "SccWrite",
            configuration.writeConcurrency(),
            configuration.writeProperty(),
            configuration.arrowConnectionInfo()
        );

    }


    <RESULT, CONFIG extends AlgoBaseConfig, ASF extends CommunityStatisticsSpecificFields> NodePropertyWriteResult<ASF> writeToDatabase(
        AlgorithmComputationResult<RESULT> algorithmResult,
        CONFIG configuration,
        NodePropertyValuesMapper<RESULT, CONFIG> nodePropertyValuesMapper,
        CommunityFunctionSupplier<RESULT> communityFunctionSupplier,
        SpecificFieldsWithCommunityStatisticsSupplier<RESULT, ASF> specificFieldsSupplier,
        StatisticsComputationInstructions statisticsComputationInstructions,
        long computeMilliseconds,
        Supplier<ASF> emptyASFSupplier,
        String procedureName,
        int writeConcurrency,
        String writeProperty,
        Optional<WriteConfig.ArrowConnectionInfo> arrowConnectionInfo
    ) {

        return algorithmResult.result().map(result -> {
            // 2. Construct NodePropertyValues from the algorithm result
            // 2.1 Should we measure some post-processing here?
            var nodePropertyValues = nodePropertyValuesMapper.map(
                result,
                configuration
            );

            // 3. Write to database
            var writeNodePropertyResult = writeNodePropertyService.write(
                algorithmResult.graph(),
                algorithmResult.graphStore(),
                nodePropertyValues,
                writeConcurrency,
                writeProperty,
                procedureName,
                arrowConnectionInfo,
                algorithmResult.algorithmTerminationFlag().get()
            );

            // 4. Compute result statistics
            var communityStatistics = CommunityStatistics.communityStats(
                nodePropertyValues.nodeCount(),
                communityFunctionSupplier.communityFunction(result),
                DefaultPool.INSTANCE,
                configuration.concurrency(),
                statisticsComputationInstructions
            );

            var componentCount = communityStatistics.componentCount();
            var communitySummary = CommunityStatistics.communitySummary(communityStatistics.histogram());

            var specificFields = specificFieldsSupplier.specificFields(result, componentCount, communitySummary);

            return NodePropertyWriteResult.<ASF>builder()
                .computeMillis(computeMilliseconds)
                .postProcessingMillis(communityStatistics.computeMilliseconds())
                .nodePropertiesWritten(writeNodePropertyResult.nodePropertiesWritten())
                .writeMillis(writeNodePropertyResult.writeMilliseconds())
                .configuration(configuration)
                .algorithmSpecificFields(specificFields)
                .build();
        }).orElseGet(() -> NodePropertyWriteResult.empty(emptyASFSupplier.get(), configuration));

    }

    <RESULT, CONFIG extends AlgoBaseConfig, ASF> NodePropertyWriteResult<ASF> writeToDatabase(
        AlgorithmComputationResult<RESULT> algorithmResult,
        CONFIG configuration,
        NodePropertyValuesMapper<RESULT, CONFIG> nodePropertyValuesMapper,
        SpecificFieldsSupplier<RESULT, ASF> specificFieldsSupplier,
        long computeMilliseconds,
        Supplier<ASF> emptyASFSupplier,
        String procedureName,
        int writeConcurrency,
        String writeProperty,
        Optional<WriteConfig.ArrowConnectionInfo> arrowConnectionInfo
    ) {

        return algorithmResult.result().map(result -> {
            // 2. Construct NodePropertyValues from the algorithm result
            // 2.1 Should we measure some post-processing here?
            var nodePropertyValues = nodePropertyValuesMapper.map(
                result,
                configuration
            );

            // 3. Write to database
            var writeNodePropertyResult = writeNodePropertyService.write(
                algorithmResult.graph(),
                algorithmResult.graphStore(),
                nodePropertyValues,
                writeConcurrency,
                writeProperty,
                procedureName,
                arrowConnectionInfo,
                algorithmResult.algorithmTerminationFlag().get()
            );


            var specificFields = specificFieldsSupplier.specificFields(result);

            return NodePropertyWriteResult.<ASF>builder()
                .computeMillis(computeMilliseconds)
                .postProcessingMillis(0)
                .nodePropertiesWritten(writeNodePropertyResult.nodePropertiesWritten())
                .writeMillis(writeNodePropertyResult.writeMilliseconds())
                .configuration(configuration)
                .algorithmSpecificFields(specificFields)
                .build();
        }).orElseGet(() -> NodePropertyWriteResult.empty(emptyASFSupplier.get(), configuration));

    }



}