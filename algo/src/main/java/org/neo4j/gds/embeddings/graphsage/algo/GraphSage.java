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
package org.neo4j.gds.embeddings.graphsage.algo;

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.model.Model;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.embeddings.graphsage.GraphSageEmbeddingsGenerator;
import org.neo4j.gds.embeddings.graphsage.GraphSageHelper;
import org.neo4j.gds.embeddings.graphsage.GraphSageModelTrainer;
import org.neo4j.gds.embeddings.graphsage.Layer;
import org.neo4j.gds.embeddings.graphsage.ModelData;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.concurrent.ExecutorService;

import static org.neo4j.gds.embeddings.graphsage.GraphSageHelper.initializeMultiLabelFeatures;
import static org.neo4j.gds.embeddings.graphsage.GraphSageHelper.initializeSingleLabelFeatures;

public class GraphSage extends Algorithm<GraphSageResult> {

    public static final String MODEL_TYPE = "graphSage";

    private final Graph graph;
    private final Model<ModelData, GraphSageTrainConfig, GraphSageModelTrainer.GraphSageTrainMetrics> model;
    private final ExecutorService executor;
    private final Concurrency concurrency;
    private final int batchSize;

    public GraphSage(
        Graph graph,
        Model<ModelData, GraphSageTrainConfig, GraphSageModelTrainer.GraphSageTrainMetrics> model,
        Concurrency concurrency,
        int batchSize,
        ExecutorService executor,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        super(progressTracker);
        this.graph = graph;
        this.concurrency = concurrency;
        this.batchSize = batchSize;
        this.model = model;
        this.executor = executor;
        this.terminationFlag = terminationFlag;
    }

    @Override
    public GraphSageResult compute() {
        Layer[] layers = model.data().layers();

        var embeddingsGenerator = new GraphSageEmbeddingsGenerator(
            layers,
            batchSize,
            concurrency,
            model.data().featureFunction(),
            model.trainConfig().randomSeed(),
            executor,
            progressTracker,
            terminationFlag
        );

        GraphSageTrainConfig trainConfig = model.trainConfig();

        var features = trainConfig.isMultiLabel()
            ? initializeMultiLabelFeatures(graph, GraphSageHelper.multiLabelFeatureExtractors(graph, trainConfig.featureProperties()))
            : initializeSingleLabelFeatures(graph, trainConfig.featureProperties());

        HugeObjectArray<double[]> embeddings = embeddingsGenerator.makeEmbeddings(
            graph,
            features
        );
        return new GraphSageResult(embeddings);
    }
}
