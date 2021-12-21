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
package org.neo4j.gds;

import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.GraphCreateConfig;
import org.neo4j.gds.config.ToMapConvertible;
import org.neo4j.gds.core.model.Model;
import org.neo4j.gds.core.model.ModelCatalog;
import org.neo4j.gds.model.ModelConfig;
import org.neo4j.gds.pipeline.ComputationResult;
import org.neo4j.gds.pipeline.ComputationResultConsumer;
import org.neo4j.gds.pipeline.validation.BeforeLoadValidation;
import org.neo4j.gds.pipeline.validation.ValidationConfiguration;
import org.neo4j.procedure.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.model.ModelConfig.MODEL_NAME_KEY;
import static org.neo4j.gds.model.ModelConfig.MODEL_TYPE_KEY;

public abstract class TrainProc<ALGO extends Algorithm<Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO>>,
    TRAIN_RESULT,
    TRAIN_CONFIG extends ModelConfig & AlgoBaseConfig,
    TRAIN_INFO extends ToMapConvertible,
    PROC_RESULT
> extends AlgoBaseProc<ALGO, Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO>, TRAIN_CONFIG, PROC_RESULT> {

    @Context
    public ModelCatalog modelCatalog;

    protected abstract String modelType();

    protected abstract PROC_RESULT constructResult(
        Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO> model,
        ComputationResult<ALGO, Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO>, TRAIN_CONFIG> computationResult
    );

    @Override
    public ComputationResultConsumer<ALGO, Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO>, TRAIN_CONFIG, Stream<PROC_RESULT>> computationResultConsumer() {
        return (computationResult, executionContext) -> {
            var model = computationResult.result();
            modelCatalog.set(model);
            return Stream.of(constructResult(model, computationResult));
        };
    }

    protected Stream<PROC_RESULT> trainAndStoreModelWithResult(ComputationResult<ALGO, Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO>, TRAIN_CONFIG> computationResult) {
        return computationResultConsumer().consume(computationResult, executionContext());
    }

    @Override
    public ValidationConfiguration<TRAIN_CONFIG> validationConfig() {
        return new ValidationConfiguration<>() {
            @Override
            public List<BeforeLoadValidation<TRAIN_CONFIG>> beforeLoadValidations() {
                return List.of(
                   new TrainingConfigValidation<>(modelCatalog, username(), modelType())
                );
            }
        };
    }

    public static class TrainingConfigValidation<TRAIN_CONFIG extends ModelConfig & AlgoBaseConfig> implements BeforeLoadValidation<TRAIN_CONFIG> {
        private final ModelCatalog modelCatalog;
        private final String username;
        private final String modelType;

        public TrainingConfigValidation(ModelCatalog modelCatalog, String username, String modelType) {
            this.modelCatalog = modelCatalog;
            this.username = username;
            this.modelType = modelType;
        }

        @Override
        public void validateConfigsBeforeLoad(
            GraphCreateConfig graphCreateConfig,
            TRAIN_CONFIG config
        ) {
            modelCatalog.verifyModelCanBeStored(
                username,
                config.modelName(),
                modelType
            );
        }
    }

    @SuppressWarnings("unused")
    public static class TrainResult {

        public final Map<String, Object> modelInfo;
        public final Map<String, Object> configuration;
        public final long trainMillis;

        public <TRAIN_RESULT, TRAIN_CONFIG extends ModelConfig & AlgoBaseConfig, TRAIN_INFO extends ToMapConvertible> TrainResult(
            Model<TRAIN_RESULT, TRAIN_CONFIG, TRAIN_INFO> trainedModel,
            long trainMillis,
            long nodeCount,
            long relationshipCount
        ) {
            TRAIN_CONFIG trainConfig = trainedModel.trainConfig();

            this.modelInfo = new HashMap<>();
            modelInfo.put(MODEL_NAME_KEY, trainedModel.name());
            modelInfo.put(MODEL_TYPE_KEY, trainedModel.algoType());
            modelInfo.putAll(trainedModel.customInfo().toMap());

            this.configuration = trainConfig.toMap();
            this.trainMillis = trainMillis;
        }
    }
}
