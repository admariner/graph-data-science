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
package org.neo4j.gds.procedures.algorithms.embeddings.stubs;

import org.neo4j.gds.applications.algorithms.embeddings.NodeEmbeddingAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.embeddings.NodeEmbeddingAlgorithmsMutateModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.embeddings.node2vec.Node2VecMutateConfig;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.procedures.algorithms.embeddings.Node2VecMutateResult;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;

import java.util.Map;
import java.util.stream.Stream;

public class LocalNode2VecMutateStub implements Node2VecMutateStub {

    private final GenericStub genericStub;
    private final NodeEmbeddingAlgorithmsEstimationModeBusinessFacade estimationModeBusinessFacade;
    private final NodeEmbeddingAlgorithmsMutateModeBusinessFacade mutateModeBusinessFacade;

    public LocalNode2VecMutateStub(
        GenericStub genericStub,
        NodeEmbeddingAlgorithmsEstimationModeBusinessFacade estimationModeBusinessFacade,
        NodeEmbeddingAlgorithmsMutateModeBusinessFacade mutateModeBusinessFacade
    ) {
        this.genericStub = genericStub;
        this.estimationModeBusinessFacade = estimationModeBusinessFacade;
        this.mutateModeBusinessFacade = mutateModeBusinessFacade;
    }

    @Override
    public Node2VecMutateConfig parseConfiguration(Map<String, Object> configuration) {
        return genericStub.parseConfiguration(Node2VecMutateConfig::of, configuration);
    }

    @Override
    public MemoryEstimation getMemoryEstimation(String username, Map<String, Object> rawConfiguration) {
        return genericStub.getMemoryEstimation(
            rawConfiguration,
            Node2VecMutateConfig::of,
            estimationModeBusinessFacade::node2Vec
        );
    }

    @Override
    public Stream<MemoryEstimateResult> estimate(Object graphNameAsString, Map<String, Object> rawConfiguration) {
        return genericStub.estimate(
            graphNameAsString,
            rawConfiguration,
            Node2VecMutateConfig::of,
            estimationModeBusinessFacade::node2Vec
        );
    }

    @Override
    public Stream<Node2VecMutateResult> execute(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        var resultBuilder = new Node2VecResultBuilderForMutateMode();

        return genericStub.execute(
            graphNameAsString,
            rawConfiguration,
            Node2VecMutateConfig::of,
            mutateModeBusinessFacade::node2Vec,
            resultBuilder
        );
    }

}
