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
package org.neo4j.gds.embeddings.graphsage;

import com.carrotsearch.hppc.LongHashSet;
import org.immutables.value.Value;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainConfig;
import org.neo4j.gds.ml.core.ComputationContext;
import org.neo4j.gds.ml.core.Variable;
import org.neo4j.gds.ml.core.batch.WeightedUniformSampler;
import org.neo4j.gds.ml.core.features.FeatureExtraction;
import org.neo4j.gds.ml.core.functions.PassthroughVariable;
import org.neo4j.gds.ml.core.functions.Weights;
import org.neo4j.gds.ml.core.optimizer.AdamOptimizer;
import org.neo4j.gds.ml.core.tensor.Matrix;
import org.neo4j.gds.ml.core.tensor.Scalar;
import org.neo4j.gds.ml.core.tensor.Tensor;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.api.ImmutableRelationshipCursor;
import org.neo4j.graphalgo.core.concurrency.ParallelUtil;
import org.neo4j.graphalgo.core.model.Model;
import org.neo4j.graphalgo.core.utils.ProgressLogger;
import org.neo4j.graphalgo.core.utils.paged.HugeObjectArray;
import org.neo4j.graphalgo.core.utils.partition.Partition;
import org.neo4j.graphalgo.core.utils.partition.PartitionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.neo4j.gds.embeddings.graphsage.GraphSageHelper.embeddings;
import static org.neo4j.gds.ml.core.RelationshipWeights.UNWEIGHTED;
import static org.neo4j.gds.ml.core.tensor.TensorFunctions.averageTensors;

public class GraphSageModelTrainer {
    private final long randomSeed;
    private Layer[] layers;
    private final boolean useWeights;
    private final double learningRate;
    private final double tolerance;
    private final int negativeSampleWeight;
    private final int concurrency;
    private final int epochs;
    private final int maxIterations;
    private final int maxSearchDepth;
    private final Function<Graph, List<LayerConfig>> layerConfigsFunction;
    private final FeatureFunction featureFunction;
    private final Collection<Weights<? extends Tensor<?>>> labelProjectionWeights;
    private final ExecutorService executor;
    private final ProgressLogger progressLogger;
    private final int batchSize;

    public GraphSageModelTrainer(GraphSageTrainConfig config, ExecutorService executor, ProgressLogger progressLogger) {
        this(config, executor, progressLogger, new SingleLabelFeatureFunction(), Collections.emptyList());
    }

    public GraphSageModelTrainer(
        GraphSageTrainConfig config,
        ExecutorService executor,
        ProgressLogger progressLogger,
        FeatureFunction featureFunction,
        Collection<Weights<? extends Tensor<?>>> labelProjectionWeights
    ) {
        this.layerConfigsFunction = graph -> config.layerConfigs(firstLayerColumns(config, graph));
        this.batchSize = config.batchSize();
        this.learningRate = config.learningRate();
        this.tolerance = config.tolerance();
        this.negativeSampleWeight = config.negativeSampleWeight();
        this.concurrency = config.concurrency();
        this.epochs = config.epochs();
        this.maxIterations = config.maxIterations();
        this.maxSearchDepth = config.searchDepth();
        this.featureFunction = featureFunction;
        this.labelProjectionWeights = labelProjectionWeights;
        this.executor = executor;
        this.progressLogger = progressLogger;
        this.useWeights = config.hasRelationshipWeightProperty();
        this.randomSeed = config.randomSeed().orElse(ThreadLocalRandom.current().nextLong());
    }

    public ModelTrainResult train(Graph graph, HugeObjectArray<double[]> features) {
        progressLogger.logStart();
        var epochLosses = new ArrayList<Double>();

        this.layers = layerConfigsFunction.apply(graph).stream()
            .map(LayerFactory::createLayer)
            .toArray(Layer[]::new);

        var weights = getWeights();
        var batchTasks = PartitionUtils.rangePartitionWithBatchSize(
            graph.nodeCount(),
            batchSize,
            batch -> new BatchTask(lossFunction(batch, graph, features), weights, tolerance)
        );

        double initialLoss = evaluateLoss(batchTasks.stream().map(BatchTask::forwardTask).collect(Collectors.toList()));
        epochLosses.add(0, initialLoss);
        double previousLoss = initialLoss;
        boolean converged = false;
        for (int epoch = 1; epoch <= epochs; epoch++) {
            var epochMessage = ":: Epoch " + epoch;
            progressLogger.logStart(epochMessage);

            double newLoss = trainEpoch(batchTasks, epoch);
            epochLosses.add(epoch, newLoss);
            progressLogger.logFinish(epochMessage);
            if (Math.abs((newLoss - previousLoss) / previousLoss) < tolerance) {
                converged = true;
                break;
            }
            previousLoss = newLoss;
        }
        progressLogger.logFinish();

        return ModelTrainResult.of(initialLoss, epochLosses, converged, this.layers);
    }

    private double trainEpoch(List<BatchTask> batchTasks, int epoch) {
        List<Weights<? extends Tensor<?>>> weights = getWeights();

        var updater = new AdamOptimizer(weights, learningRate);

        // Change: no generateNewRandomState for each batch

        double totalLoss = Double.NaN;
        int iteration = 1;
        for (;iteration <= maxIterations; iteration++) {
            progressLogger.logStart(":: Iteration " + iteration);

            // run forward + maybe backward for each Batch
            ParallelUtil.run(batchTasks, executor);
            totalLoss = batchTasks.stream().mapToDouble(BatchTask::loss).average().orElseThrow();

            var converged = batchTasks.stream().allMatch(task -> task.converged);
            if (converged) {
                progressLogger.logFinish(":: Iteration " + iteration);
                break;
            }

            var batchedGradients = batchTasks
                .stream()
                .map(BatchTask::weightGradients)
                .collect(Collectors.toList());

            var meanGradients = averageTensors(batchedGradients);

            updater.update(meanGradients);

            progressLogger.getLog().debug(
                "Epoch %d LOSS: %.10f at iteration %d",
                epoch,
                totalLoss,
                iteration
            );

            progressLogger.logFinish(":: Iteration " + iteration);
        }

        return totalLoss;
    }

    static class BatchTask implements Runnable {

        private final Variable<Scalar> lossFunction;
        private final List<Weights<? extends Tensor<?>>> weightVariables;
        private List<? extends Tensor<?>> weightGradients;
        private final double tolerance;
        private boolean converged;
        private double prevLoss;

        BatchTask(
            Variable<Scalar> lossFunction,
            List<Weights<? extends Tensor<?>>> weightVariables,
            double tolerance
        ) {
            this.lossFunction = lossFunction;
            this.weightVariables = weightVariables;
            this.tolerance = tolerance;
            this.prevLoss = Double.MAX_VALUE;
        }

        @Override
        public void run() {
            var localCtx = new ComputationContext();
            var loss = localCtx.forward(lossFunction).value();

            converged = Math.abs(prevLoss - loss) < tolerance;
            prevLoss = loss;
            if (!converged) {
                localCtx.backward(lossFunction);
                weightGradients = weightVariables.stream().map(localCtx::gradient).collect(Collectors.toList());
            }
        }

        public boolean converged() {
            return converged;
        }

        public double loss() {
            return prevLoss;
        }

        public List<? extends Tensor<?>> weightGradients() {
            return weightGradients;
        }

        BatchForwardTask forwardTask() {
            return new BatchForwardTask(lossFunction);
        }
    }

    static class BatchForwardTask implements Runnable {

        private final Variable<Scalar> lossFunction;
        private double loss;

        BatchForwardTask(Variable<Scalar> lossFunction) {
            this.lossFunction = lossFunction;
            this.loss = Double.MAX_VALUE;
        }

        @Override
        public void run() {
            var localCtx = new ComputationContext();
            loss = localCtx.forward(lossFunction).value();
        }

        public double loss() {
            return loss;
        }
    }

    private double evaluateLoss(List<BatchForwardTask> batchTasks) {

        ParallelUtil.run(batchTasks, executor);

        var lossValue = batchTasks.stream().mapToDouble(BatchForwardTask::loss).average().orElseThrow();
        progressLogger.getLog().debug("Initial Loss: %s", lossValue);
        return lossValue;
    }

    private Variable<Scalar> lossFunction(Partition batch, Graph graph, HugeObjectArray<double[]> features) {
        var batchLocalRandomSeed = getBatchIndex(batch) + randomSeed;

        var neighbours = neighborBatch(graph, batch, batchLocalRandomSeed).toArray();

        var neighborsSet = new LongHashSet(neighbours.length);
        neighborsSet.addAll(neighbours);

        var totalBatch = LongStream.concat(
            batch.stream(),
            LongStream.concat(
                Arrays.stream(neighbours),
                // batch.nodeCount is <= config.batchsize (which is an int)
                negativeBatch(graph, Math.toIntExact(batch.nodeCount()), neighborsSet, batchLocalRandomSeed)
            )
        ).toArray();

        Variable<Matrix> embeddingVariable = embeddings(graph, useWeights, totalBatch, features, this.layers, featureFunction);

        Variable<Scalar> lossFunction = new GraphSageLoss(
            useWeights ? graph::relationshipProperty : UNWEIGHTED,
            embeddingVariable,
            totalBatch,
            negativeSampleWeight
        );

        return new PassthroughVariable<>(lossFunction);
    }

    LongStream neighborBatch(Graph graph, Partition batch, long batchLocalSeed) {
        var neighborBatchBuilder = LongStream.builder();
        var localRandom = new Random(batchLocalSeed);

        batch.consume(nodeId -> {
            // randomWalk with at most maxSearchDepth steps and only save last node
            int searchDepth = localRandom.nextInt(maxSearchDepth) + 1;
            AtomicLong currentNode = new AtomicLong(nodeId);
            while (searchDepth > 0) {
                NeighborhoodSampler neighborhoodSampler = new NeighborhoodSampler(currentNode.get() + searchDepth);
                OptionalLong maybeSample = neighborhoodSampler.sampleOne(graph, nodeId);
                if (maybeSample.isPresent()) {
                    currentNode.set(maybeSample.getAsLong());
                } else {
                    // terminate
                    searchDepth = 0;
                }
                searchDepth--;
            }
            neighborBatchBuilder.add(currentNode.get());
        });

        return neighborBatchBuilder.build();
    }

    // get a negative sample per node in batch
    LongStream negativeBatch(Graph graph, int batchSize, LongHashSet neighbours, long batchLocalRandomSeed) {
        long nodeCount = graph.nodeCount();
        var sampler = new WeightedUniformSampler(batchLocalRandomSeed);

        // each node should be possible to sample
        // therefore we need fictive rels to all nodes
        // Math.log to avoid always sampling the high degree nodes
        var degreeWeightedNodes = LongStream.range(0, nodeCount)
            .mapToObj(nodeId -> ImmutableRelationshipCursor.of(0, nodeId, Math.pow(graph.degree(nodeId), 0.75)));

        return sampler.sample(degreeWeightedNodes, nodeCount, batchSize, sample -> !neighbours.contains(sample));
    }

    private List<Weights<? extends Tensor<?>>> getWeights() {
        List<Weights<? extends Tensor<?>>> weights = new ArrayList<>(labelProjectionWeights);
        weights.addAll(Arrays.stream(layers)
            .flatMap(layer -> layer.weights().stream())
            .collect(Collectors.toList()));
        return weights;
    }

    private static int getBatchIndex(Partition partition) {
        return Math.toIntExact(Math.floorDiv(partition.startNode(), partition.nodeCount()));
    }

    private int firstLayerColumns(GraphSageTrainConfig config, Graph graph) {
        return config.projectedFeatureDimension().orElseGet(() -> {
            var featureExtractors = GraphSageHelper.featureExtractors(graph, config);
            return FeatureExtraction.featureCount(featureExtractors);
        });
    }

    @ValueClass
    public interface GraphSageTrainMetrics extends Model.Mappable {
        List<Double> epochLosses();
        boolean didConverge();

        @Value.Derived
        default int ranEpochs() {
            // Exclude the `initialLoss`
            return epochLosses().isEmpty()
                ? 0
                : epochLosses().size() - 1;
        }

        @Override
        @Value.Auxiliary
        @Value.Derived
        default Map<String, Object> toMap() {
            return Map.of(
                "metrics", Map.of(
                    "epochLosses", epochLosses(),
                    "didConverge", didConverge(),
                    "ranEpochs", ranEpochs()
            ));
        }
    }

    @ValueClass
    public interface ModelTrainResult {

        GraphSageTrainMetrics metrics();

        Layer[] layers();

        static ModelTrainResult of(
            double startLoss,
            List<Double> epochLosses,
            boolean converged,
            Layer[] layers
        ) {
            return ImmutableModelTrainResult.builder()
                .layers(layers)
                .metrics(ImmutableGraphSageTrainMetrics.of(epochLosses, converged))
                .build();
        }
    }
}
