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
package org.neo4j.gds.ml.pipeline.nodePipeline;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.ml.models.TrainingMethod;
import org.neo4j.gds.ml.models.automl.TunableTrainerConfig;
import org.neo4j.gds.ml.pipeline.NodePropertyStep;
import org.neo4j.gds.ml.pipeline.TestGdsCallableFinder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NodeClassificationPipelineTest {

    @Test
    void canCreateEmptyPipeline() {
        var pipeline = new NodeClassificationTrainingPipeline();

        assertThat(pipeline)
            .returns(List.of(), NodeClassificationTrainingPipeline::featureSteps)
            .returns(List.of(), NodeClassificationTrainingPipeline::nodePropertySteps)
            .returns(NodeClassificationSplitConfig.DEFAULT_CONFIG, NodeClassificationTrainingPipeline::splitConfig);

        assertThat(pipeline.trainingParameterSpace())
            .isEqualTo(Map.of(TrainingMethod.LogisticRegression, List.of(), TrainingMethod.RandomForest, List.of()));
    }

    @Test
    void canSelectFeature() {
        var pipeline = new NodeClassificationTrainingPipeline();
        var fooStep = new NodeClassificationFeatureStep("foo");
        pipeline.addFeatureStep(fooStep);

        assertThat(pipeline)
            .returns(List.of(fooStep), NodeClassificationTrainingPipeline::featureSteps);

        var barStep = new NodeClassificationFeatureStep("bar");
        pipeline.addFeatureStep(barStep);

        assertThat(pipeline)
            .returns(List.of(fooStep, barStep), NodeClassificationTrainingPipeline::featureSteps);
    }

    @Test
    void canAddNodePropertySteps() {
        var pipeline = new NodeClassificationTrainingPipeline();
        var pageRankPropertyStep = new NodePropertyStep(
            TestGdsCallableFinder.findByName("gds.testProc.mutate").orElseThrow(),
            Map.of("mutateProperty", "prop1")
        );

        pipeline.addNodePropertyStep(pageRankPropertyStep);

        assertThat(pipeline)
            .returns(List.of(pageRankPropertyStep), NodeClassificationTrainingPipeline::nodePropertySteps);

        var degreeNodePropertyStep = new NodePropertyStep(
            TestGdsCallableFinder.findByName("gds.testProc.mutate").orElseThrow(),
            Map.of("mutateProperty", "prop2")
        );

        pipeline.addNodePropertyStep(degreeNodePropertyStep);

        assertThat(pipeline)
            .returns(List.of(pageRankPropertyStep, degreeNodePropertyStep), NodeClassificationTrainingPipeline::nodePropertySteps);
    }

    @Test
    void canSetParameterSpace() {
        var config = TunableTrainerConfig.of(Map.of("penalty", 19), TrainingMethod.LogisticRegression);

        var pipeline = new NodeClassificationTrainingPipeline();
        pipeline.setTrainingParameterSpace(TrainingMethod.LogisticRegression, List.of(config));

        assertThat(pipeline.trainingParameterSpace().get(TrainingMethod.LogisticRegression)).containsExactly(config);
    }

    @Test
    void overridesTheParameterSpace() {
        var config1 = TunableTrainerConfig.of(Map.of("penalty", 19), TrainingMethod.LogisticRegression);
        var config2 = TunableTrainerConfig.of(Map.of("penalty", 1337), TrainingMethod.LogisticRegression);
        var config3 = TunableTrainerConfig.of(Map.of("penalty", 42), TrainingMethod.LogisticRegression);

        var pipeline = new NodeClassificationTrainingPipeline();
        pipeline.setTrainingParameterSpace(TrainingMethod.LogisticRegression, List.of(config1));
        pipeline.setTrainingParameterSpace(TrainingMethod.LogisticRegression, List.of(config2, config3));

        var parameterSpace = pipeline.trainingParameterSpace();
        assertThat(parameterSpace.get(TrainingMethod.LogisticRegression)).containsExactly(config2, config3);
    }

    @Test
    void canSetSplitConfig() {
        var pipeline = new NodeClassificationTrainingPipeline();
        var splitConfig = NodeClassificationSplitConfig.testBuilder().testFraction(0.555).build();
        pipeline.setSplitConfig(splitConfig);

        assertThat(pipeline)
            .returns(splitConfig, NodeClassificationTrainingPipeline::splitConfig);
    }

    @Test
    void overridesTheSplitConfig() {
        var pipeline = new NodeClassificationTrainingPipeline();
        var splitConfig = NodeClassificationSplitConfig.testBuilder().testFraction(0.5).build();
        pipeline.setSplitConfig(splitConfig);

        var splitConfigOverride = NodeClassificationSplitConfig.testBuilder().testFraction(0.7).build();
        pipeline.setSplitConfig(splitConfigOverride);

        assertThat(pipeline)
            .returns(splitConfigOverride, NodeClassificationTrainingPipeline::splitConfig);
    }

    @Nested
    class ToMapTest {

        @Test
        void returnsCorrectDefaultsMap() {
            var pipeline = new NodeClassificationTrainingPipeline();
            assertThat(pipeline.toMap())
                .containsOnlyKeys("featurePipeline", "splitConfig", "trainingParameterSpace")
                .satisfies(pipelineMap -> assertThat(pipelineMap.get("featurePipeline"))
                    .isInstanceOf(Map.class)
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsOnlyKeys("nodePropertySteps", "featureProperties")
                    .returns(List.of(), featurePipelineMap -> featurePipelineMap.get("nodePropertySteps"))
                    .returns(List.of(), featurePipelineMap -> featurePipelineMap.get("featureProperties")))
                .returns(
                    NodeClassificationSplitConfig.DEFAULT_CONFIG.toMap(),
                    pipelineMap -> pipelineMap.get("splitConfig")
                )
                .returns(
                    Map.of(TrainingMethod.LogisticRegression.name(), List.of(), TrainingMethod.RandomForest.name(), List.of()),
                    pipelineMap -> pipelineMap.get("trainingParameterSpace")
                );
        }

        @Test
        void returnsCorrectMapWithFullConfiguration() {
            var pipeline = new NodeClassificationTrainingPipeline();
            var nodePropertyStep = new NodePropertyStep(
                TestGdsCallableFinder.findByName("gds.testProc.mutate").orElseThrow(),
                Map.of("mutateProperty", "prop1")
            );
            pipeline.addNodePropertyStep(nodePropertyStep);

            var fooStep = new NodeClassificationFeatureStep("foo");
            pipeline.addFeatureStep(fooStep);

            pipeline.setTrainingParameterSpace(TrainingMethod.LogisticRegression, List.of(
                TunableTrainerConfig.of(Map.of("penalty", 1000000), TrainingMethod.LogisticRegression),
                TunableTrainerConfig.of(Map.of("penalty", 1), TrainingMethod.LogisticRegression)
            ));

            var splitConfig = NodeClassificationSplitConfig.testBuilder().testFraction(0.5).build();
            pipeline.setSplitConfig(splitConfig);

            assertThat(pipeline.toMap())
                .containsOnlyKeys("featurePipeline", "splitConfig", "trainingParameterSpace")
                .satisfies(pipelineMap -> assertThat(pipelineMap.get("featurePipeline"))
                    .isInstanceOf(Map.class)
                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                    .containsOnlyKeys("nodePropertySteps", "featureProperties")
                    .returns(
                        List.of(nodePropertyStep.toMap()),
                        featurePipelineMap -> featurePipelineMap.get("nodePropertySteps")
                    )
                    .returns(
                        List.of(fooStep.toMap()),
                        featurePipelineMap -> featurePipelineMap.get("featureProperties")
                    ))
                .returns(
                    pipeline.splitConfig().toMap(),
                    pipelineMap -> pipelineMap.get("splitConfig")
                )
                .returns(
                    pipeline.trainingParameterSpace().get(TrainingMethod.LogisticRegression)
                        .stream()
                        .map(tunableTrainerConfig -> tunableTrainerConfig.value)
                        .collect(Collectors.toList()),
                    pipelineMap -> ((Map<String, Object>) pipelineMap.get("trainingParameterSpace")).get(TrainingMethod.LogisticRegression.name())
                );
        }
    }

    @Nested
    class CopyPipelineTest {

        @Test
        void deepCopiesFeatureSteps() {
            var pipeline = new NodeClassificationTrainingPipeline();
            var fooStep = new NodeClassificationFeatureStep("foo");
            pipeline.addFeatureStep(fooStep);

            var copy = pipeline.copy();
            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> assertThat(copiedPipeline.featureSteps())
                    .isNotSameAs(pipeline.featureSteps())
                    .containsExactly(fooStep));

            var barStep = new NodeClassificationFeatureStep("bar");
            pipeline.addFeatureStep(barStep);

            assertThat(copy.featureSteps()).doesNotContain(barStep);
        }

        @Test
        void deepCopiesNodePropertySteps() {
            var pipeline = new NodeClassificationTrainingPipeline();
            var nodePropertyStep = new NodePropertyStep(
                TestGdsCallableFinder.findByName("gds.testProc.mutate").orElseThrow(),
                Map.of("mutateProperty", "prop1")
            );
            pipeline.addNodePropertyStep(nodePropertyStep);

            var copy = pipeline.copy();
            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> assertThat(copiedPipeline.nodePropertySteps())
                    .isNotSameAs(pipeline.nodePropertySteps())
                    .containsExactly(nodePropertyStep));

            var otherNodePropertyStep = new NodePropertyStep(
                TestGdsCallableFinder.findByName("gds.testProc.mutate").orElseThrow(),
                Map.of("mutateProperty", "prop2")
            );
            pipeline.addNodePropertyStep(otherNodePropertyStep);

            assertThat(copy.nodePropertySteps()).doesNotContain(otherNodePropertyStep);
        }

        @Test
        void deepCopiesParameterSpace() {
            var pipeline = new NodeClassificationTrainingPipeline();
            pipeline.setTrainingParameterSpace(TrainingMethod.LogisticRegression, List.of(
                TunableTrainerConfig.of(Map.of("penalty", 1000000), TrainingMethod.LogisticRegression),
                TunableTrainerConfig.of(Map.of("penalty", 1), TrainingMethod.LogisticRegression)
            ));

            var copy = pipeline.copy();

            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> {
                    var copiedParameterSpace = copiedPipeline.trainingParameterSpace();
                    var originalParameterSpace = pipeline.trainingParameterSpace();
                    assertThat(copiedParameterSpace.get(TrainingMethod.LogisticRegression))
                        // Look at the pipeline because there are some defaults are added behind the scene.
                        .isNotSameAs(originalParameterSpace)
                        .containsExactlyInAnyOrderElementsOf(originalParameterSpace.get(TrainingMethod.LogisticRegression));
                });
        }

        @Test
        void doesntDeepCopySplitConfig() {
            var pipeline = new NodeClassificationTrainingPipeline();
            var splitConfig = NodeClassificationSplitConfig.testBuilder().testFraction(0.5).build();
            pipeline.setSplitConfig(splitConfig);

            var copy = pipeline.copy();

            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> assertThat(copiedPipeline.splitConfig()).isSameAs(splitConfig));
        }
    }
}
