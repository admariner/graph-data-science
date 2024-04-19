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
package org.neo4j.gds.applications.algorithms.similarity;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.applications.algorithms.machinery.ProgressTrackerCreator;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.similarity.filteredknn.FilteredKnn;
import org.neo4j.gds.similarity.filteredknn.FilteredKnnBaseConfig;
import org.neo4j.gds.similarity.filteredknn.FilteredKnnResult;
import org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityBaseConfig;
import org.neo4j.gds.similarity.filtering.NodeFilter;
import org.neo4j.gds.similarity.knn.ImmutableKnnContext;
import org.neo4j.gds.similarity.knn.Knn;
import org.neo4j.gds.similarity.knn.KnnBaseConfig;
import org.neo4j.gds.similarity.knn.KnnContext;
import org.neo4j.gds.similarity.knn.KnnFactory;
import org.neo4j.gds.similarity.knn.KnnNeighborFilterFactory;
import org.neo4j.gds.similarity.knn.KnnResult;
import org.neo4j.gds.similarity.knn.metrics.SimilarityComputer;
import org.neo4j.gds.similarity.nodesim.NodeSimilarity;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityBaseConfig;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityResult;
import org.neo4j.gds.wcc.WccAlgorithmFactory;

import java.util.List;

import static org.neo4j.gds.applications.algorithms.similarity.AlgorithmLabels.FILTERED_NODE_SIMILARITY;
import static org.neo4j.gds.applications.algorithms.similarity.AlgorithmLabels.KNN;
import static org.neo4j.gds.applications.algorithms.similarity.AlgorithmLabels.NODE_SIMILARITY;

public class SimilarityAlgorithms {
    private final ProgressTrackerCreator progressTrackerCreator;

    public SimilarityAlgorithms(ProgressTrackerCreator progressTrackerCreator) {
        this.progressTrackerCreator = progressTrackerCreator;
    }

    FilteredKnnResult filteredKnn(Graph graph, FilteredKnnBaseConfig configuration) {
        var taskTree = KnnFactory.knnTaskTree(graph.nodeCount(), configuration.maxIterations());
        var progressTracker = progressTrackerCreator.createProgressTracker(
            configuration,
            taskTree
        );
        var knnContext = ImmutableKnnContext
            .builder()
            .progressTracker(progressTracker)
            .executor(DefaultPool.INSTANCE)
            .build();

        var filteredKnn = selectAlgorithmConfiguration(graph, configuration, knnContext);

        return filteredKnn.compute();
    }

    NodeSimilarityResult filteredNodeSimilarity(Graph graph, FilteredNodeSimilarityBaseConfig configuration) {
        var sourceNodeFilter = configuration.sourceNodeFilter().toNodeFilter(graph);
        var targetNodeFilter = configuration.targetNodeFilter().toNodeFilter(graph);

        var task = Tasks.task(
            FILTERED_NODE_SIMILARITY,
            filteredNodeSimilarityProgressTask(graph, configuration.useComponents().computeComponents()),
            Tasks.leaf("compare node pairs")
        );

        var progressTracker = progressTrackerCreator.createProgressTracker(configuration, task);

        var algorithm = new NodeSimilarity(
            graph,
            configuration.toParameters(),
            configuration.typedConcurrency(),
            DefaultPool.INSTANCE,
            progressTracker,
            sourceNodeFilter,
            targetNodeFilter
        );

        return algorithm.compute();
    }

    KnnResult knn(Graph graph, KnnBaseConfig configuration) {
        var parameters = configuration.toParameters().finalize(graph.nodeCount());

        long nodeCount = graph.nodeCount();

        Task task = Tasks.task(
            KNN,
            Tasks.leaf("Initialize random neighbors", nodeCount),
            Tasks.iterativeDynamic(
                "Iteration",
                () -> List.of(
                    Tasks.leaf("Split old and new neighbors", nodeCount),
                    Tasks.leaf("Reverse old and new neighbors", nodeCount),
                    Tasks.leaf("Join neighbors", nodeCount)
                ),
                configuration.maxIterations()
            )
        );


        var progressTracker = progressTrackerCreator.createProgressTracker(configuration, task);

        var algorithm = Knn.create(
            graph,
            parameters,
            SimilarityComputer.ofProperties(graph, parameters.nodePropertySpecs()),
            new KnnNeighborFilterFactory(graph.nodeCount()),
            ImmutableKnnContext
                .builder()
                .progressTracker(progressTracker)
                .executor(DefaultPool.INSTANCE)
                .build()
        );

        return algorithm.compute();
    }

    NodeSimilarityResult nodeSimilarity(Graph graph, NodeSimilarityBaseConfig configuration) {
        Task task = Tasks.task(
            NODE_SIMILARITY,
            configuration.useComponents().computeComponents()
                ? Tasks.task(
                "prepare",
                new WccAlgorithmFactory<>().progressTask(graph),
                Tasks.leaf("initialize", graph.relationshipCount())
            )
                : Tasks.leaf("prepare", graph.relationshipCount()),
            Tasks.leaf("compare node pairs")
        );

        var progressTracker = progressTrackerCreator.createProgressTracker(configuration, task);

        var algorithm = new NodeSimilarity(
            graph,
            configuration.toParameters(),
            configuration.typedConcurrency(),
            DefaultPool.INSTANCE,
            progressTracker,
            NodeFilter.ALLOW_EVERYTHING,
            NodeFilter.ALLOW_EVERYTHING
        );

        return algorithm.compute();
    }

    private Task filteredNodeSimilarityProgressTask(Graph graph, boolean runWcc) {
        if (runWcc) {
            return Tasks.task(
                "prepare",
                new WccAlgorithmFactory<>().progressTask(graph),
                Tasks.leaf("initialize", graph.relationshipCount())
            );
        }
        return Tasks.leaf("prepare", graph.relationshipCount());
    }

    private static FilteredKnn selectAlgorithmConfiguration(
        Graph graph,
        FilteredKnnBaseConfig configuration,
        KnnContext knnContext
    ) {
        if (configuration.seedTargetNodes()) {
            return FilteredKnn.createWithDefaultSeeding(graph, configuration, knnContext);
        }

        return FilteredKnn.createWithoutSeeding(graph, configuration, knnContext);
    }
}