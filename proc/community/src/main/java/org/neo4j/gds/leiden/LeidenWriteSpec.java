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
package org.neo4j.gds.leiden;

import org.neo4j.gds.NullComputationResultConsumer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.ExecutionMode;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.procedures.algorithms.configuration.NewConfigFunction;
import org.neo4j.gds.procedures.community.leiden.LeidenWriteResult;

import java.util.stream.Stream;

import static org.neo4j.gds.leiden.Constants.LEIDEN_DESCRIPTION;


@GdsCallable(name = "gds.leiden.write", aliases = {"gds.beta.leiden.write"}, description = LEIDEN_DESCRIPTION, executionMode = ExecutionMode.WRITE_NODE_PROPERTY)
public class LeidenWriteSpec implements AlgorithmSpec<Leiden, LeidenResult, LeidenWriteConfig, Stream<LeidenWriteResult>, LeidenAlgorithmFactory<LeidenWriteConfig>> {
    @Override
    public String name() {
        return "LeidenWrite";
    }

    @Override
    public LeidenAlgorithmFactory<LeidenWriteConfig> algorithmFactory(ExecutionContext executionContext) {
        return new LeidenAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<LeidenWriteConfig> newConfigFunction() {
        return (__, config) -> LeidenWriteConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<Leiden, LeidenResult, LeidenWriteConfig, Stream<LeidenWriteResult>> computationResultConsumer() {
        return new NullComputationResultConsumer<>();
    }
}
