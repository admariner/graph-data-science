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
package org.neo4j.gds.ml.linkmodels.pipeline;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.ml.linkmodels.pipeline.linkFeatures.linkfunctions.CosineFeatureStep;
import org.neo4j.gds.ml.linkmodels.pipeline.linkFeatures.linkfunctions.HadamardFeatureStep;
import org.neo4j.gds.ml.linkmodels.pipeline.logisticRegression.LinkLogisticRegressionTrainConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingPipelineTest {

    private TrainingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new TrainingPipeline();
    }

    @Test
    void canCreateEmptyPipeline() {
        assertThat(pipeline)
            .returns(List.of(), TrainingPipeline::featureSteps)
            .returns(List.of(), TrainingPipeline::nodePropertySteps)
            .returns(LinkPredictionSplitConfig.DEFAULT_CONFIG, TrainingPipeline::splitConfig)
            .returns(
                List.of(LinkLogisticRegressionTrainConfig.defaultConfig().toMap()),
                TrainingPipeline::parameterSpace
            );
    }

    @Test
    void canAddFeatureSteps() {
        var hadamardFeatureStep = new HadamardFeatureStep(List.of("a"));
        pipeline.addFeatureStep(hadamardFeatureStep);

        assertThat(pipeline)
            .returns(List.of(hadamardFeatureStep), TrainingPipeline::featureSteps);

        var cosineFeatureStep = new CosineFeatureStep(List.of("b", "c"));
        pipeline.addFeatureStep(cosineFeatureStep);

        assertThat(pipeline)
            .returns(List.of(hadamardFeatureStep, cosineFeatureStep), TrainingPipeline::featureSteps);
    }

    @Test
    void canAddNodePropertySteps() {
        var pageRankPropertyStep = NodePropertyStep.of("pageRank", Map.of("mutateProperty", "pr"));
        pipeline.addNodePropertyStep(pageRankPropertyStep);

        assertThat(pipeline)
            .returns(List.of(pageRankPropertyStep), TrainingPipeline::nodePropertySteps);

        var degreeNodePropertyStep = NodePropertyStep.of("degree", Map.of("mutateProperty", "degree"));
        pipeline.addNodePropertyStep(degreeNodePropertyStep);

        assertThat(pipeline)
            .returns(List.of(pageRankPropertyStep, degreeNodePropertyStep), TrainingPipeline::nodePropertySteps);
    }

    @Test
    void canSetParameterSpace() {
        pipeline.setParameterSpace(List.of(
            Map.of("penalty", 19)
        ));

        assertThat(pipeline)
            .returns(List.of(Map.of(
                "batchSize", 100,
                "maxEpochs", 100,
                "minEpochs", 1,
                "patience", 1,
                "penalty", 19.0,
                "tolerance", 0.001,
                "useBiasFeature", true
            )), TrainingPipeline::parameterSpace);
    }

    @Test
    void overridesTheParameterSpace() {
        pipeline.setParameterSpace(List.of(
            Map.of("penalty", 19)
        ));

        pipeline.setParameterSpace(List.of(
            Map.of("penalty", 1337),
            Map.of("penalty", 42)
        ));

        assertThat(pipeline)
            .returns(List.of(
                Map.of(
                "batchSize", 100,
                "maxEpochs", 100,
                "minEpochs", 1,
                "patience", 1,
                "penalty", 1337.0,
                "tolerance", 0.001,
                "useBiasFeature", true
            ), Map.of(
                "batchSize", 100,
                "maxEpochs", 100,
                "minEpochs", 1,
                "patience", 1,
                "penalty", 42.0,
                "tolerance", 0.001,
                "useBiasFeature", true
            )), TrainingPipeline::parameterSpace);
    }

    @Test
    void canSetSplitConfig() {
        var splitConfig = LinkPredictionSplitConfig.builder().trainFraction(0.01).testFraction(0.5).build();
        pipeline.setSplitConfig(splitConfig);

        assertThat(pipeline)
            .returns(splitConfig, TrainingPipeline::splitConfig);
    }

    @Test
    void overridesTheSplitConfig() {
        var splitConfig = LinkPredictionSplitConfig.builder().trainFraction(0.01).testFraction(0.5).build();
        pipeline.setSplitConfig(splitConfig);

        var splitConfigOverride = LinkPredictionSplitConfig.builder().trainFraction(0.1).testFraction(0.7).build();
        pipeline.setSplitConfig(splitConfigOverride);

        assertThat(pipeline)
            .returns(splitConfigOverride, TrainingPipeline::splitConfig);
    }

    @Nested
    class ToMapTest {

        @Test
        void returnsCorrectDefaultsMap() {
            assertThat(pipeline.toMap())
                .containsOnlyKeys("featurePipeline", "splitConfig", "parameterSpace")
                .satisfies(pipelineMap -> {
                    assertThat(pipelineMap.get("featurePipeline"))
                        .isInstanceOf(Map.class)
                        .asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsOnlyKeys("nodePropertySteps", "featureSteps")
                        .returns(List.of(), featurePipelineMap -> featurePipelineMap.get("nodePropertySteps"))
                        .returns(List.of(), featurePipelineMap -> featurePipelineMap.get("featureSteps"));
                })
                .returns(
                    LinkPredictionSplitConfig.DEFAULT_CONFIG.toMap(),
                    pipelineMap -> pipelineMap.get("splitConfig")
                )
                .returns(
                    List.of(LinkLogisticRegressionTrainConfig.defaultConfig().toMap()),
                    pipelineMap -> pipelineMap.get("parameterSpace")
                );
        }

        @Test
        void returnsCorrectMapWithFullConfiguration() {
            var pageRankPropertyStep = NodePropertyStep.of("pageRank", Map.of("mutateProperty", "pr"));
            pipeline.addNodePropertyStep(pageRankPropertyStep);

            var hadamardFeatureStep = new HadamardFeatureStep(List.of("a"));
            pipeline.addFeatureStep(hadamardFeatureStep);

            pipeline.setParameterSpace(List.of(
                Map.of("penalty", 1000000),
                Map.of("penalty", 1)
            ));

            var splitConfig = LinkPredictionSplitConfig.builder().trainFraction(0.01).testFraction(0.5).build();
            pipeline.setSplitConfig(splitConfig);

            assertThat(pipeline.toMap())
                .containsOnlyKeys("featurePipeline", "splitConfig", "parameterSpace")
                .satisfies(pipelineMap -> {
                    assertThat(pipelineMap.get("featurePipeline"))
                        .isInstanceOf(Map.class)
                        .asInstanceOf(InstanceOfAssertFactories.MAP)
                        .containsOnlyKeys("nodePropertySteps", "featureSteps")
                        .returns(List.of(pageRankPropertyStep.toMap()), featurePipelineMap -> featurePipelineMap.get("nodePropertySteps"))
                        .returns(List.of(hadamardFeatureStep.toMap()), featurePipelineMap -> featurePipelineMap.get("featureSteps"));
                })
                .returns(
                    pipeline.splitConfig().toMap(),
                    pipelineMap -> pipelineMap.get("splitConfig")
                )
                .returns(
                    pipeline.parameterSpace(),
                    pipelineMap -> pipelineMap.get("parameterSpace")
                );
        }
    }

    @Nested
    class CopyPipelineTest {

        @Test
        void deepCopiesFeatureSteps() {
            var hadamardFeatureStep = new HadamardFeatureStep(List.of("a"));
            pipeline.addFeatureStep(hadamardFeatureStep);

            var copy = pipeline.copy();
            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> assertThat(copiedPipeline.featureSteps())
                    .isNotSameAs(pipeline.featureSteps())
                    .containsExactly(hadamardFeatureStep));

            var cosineFeatureStep = new CosineFeatureStep(List.of("b", "c"));
            pipeline.addFeatureStep(cosineFeatureStep);

            assertThat(copy.featureSteps()).doesNotContain(cosineFeatureStep);
        }

        @Test
        void deepCopiesNodePropertySteps() {
            var pageRankPropertyStep = NodePropertyStep.of("pageRank", Map.of("mutateProperty", "pr"));
            pipeline.addNodePropertyStep(pageRankPropertyStep);

            var copy = pipeline.copy();
            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> assertThat(copiedPipeline.nodePropertySteps())
                    .isNotSameAs(pipeline.nodePropertySteps())
                    .containsExactly(pageRankPropertyStep));

            var degreeNodePropertyStep = NodePropertyStep.of("degree", Map.of("mutateProperty", "degree"));
            pipeline.addNodePropertyStep(degreeNodePropertyStep);

            assertThat(copy.nodePropertySteps()).doesNotContain(degreeNodePropertyStep);
        }

        @Test
        void deepCopiesParameterSpace() {
            pipeline.setParameterSpace(List.of(
                Map.of("penalty", 1000000),
                Map.of("penalty", 1)
            ));

            var copy = pipeline.copy();

            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> {
                    var copiedParameterSpace = copiedPipeline.parameterSpace();
                    var originalParameterSpace = pipeline.parameterSpace();
                    assertThat(copiedParameterSpace)
                        // Look at the pipeline because there are some defaults are added behind the scene.
                        .isNotSameAs(originalParameterSpace)
                        .containsExactlyInAnyOrderElementsOf(originalParameterSpace);
                });
        }

        @Test
        void doesntDeepCopySplitConfig() {
            var splitConfig = LinkPredictionSplitConfig.builder().trainFraction(0.01).testFraction(0.5).build();
            pipeline.setSplitConfig(splitConfig);

            var copy = pipeline.copy();

            assertThat(copy)
                .isNotSameAs(pipeline)
                .satisfies(copiedPipeline -> {
                    assertThat(copiedPipeline.splitConfig()).isSameAs(splitConfig);
                });
        }
    }
}
