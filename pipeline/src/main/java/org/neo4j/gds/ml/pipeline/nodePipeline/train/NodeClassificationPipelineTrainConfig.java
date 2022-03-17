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
package org.neo4j.gds.ml.pipeline.nodePipeline.train;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RandomSeedConfig;
import org.neo4j.gds.config.TargetNodePropertyConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.metrics.MetricSpecification;
import org.neo4j.gds.model.ModelConfig;

import java.util.List;

@ValueClass
@Configuration
@SuppressWarnings("immutables:subtype")
public interface NodeClassificationPipelineTrainConfig extends AlgoBaseConfig, ModelConfig, RandomSeedConfig, TargetNodePropertyConfig {

    long serialVersionUID = 0x42L;

    String graphName();

    @Configuration.ConvertWith("org.neo4j.gds.metrics.MetricSpecification#parse")
    @Configuration.ToMapValue("org.neo4j.gds.metrics.MetricSpecification#specificationsToString")
    List<MetricSpecification> metrics();

    String pipeline();

    static NodeClassificationPipelineTrainConfig of(String username, CypherMapWrapper config) {
        return new NodeClassificationPipelineTrainConfigImpl(username, config);
    }

    static ImmutableNodeClassificationPipelineTrainConfig.Builder builder() {
        return ImmutableNodeClassificationPipelineTrainConfig.builder();
    }
}
