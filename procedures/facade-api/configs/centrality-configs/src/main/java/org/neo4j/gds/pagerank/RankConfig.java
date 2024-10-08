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
package org.neo4j.gds.pagerank;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.beta.pregel.Partitioning;
import org.neo4j.gds.beta.pregel.PregelConfig;
import org.neo4j.gds.config.SourceNodesConfig;
import org.neo4j.gds.config.ToleranceConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.scaling.NoneScaler;
import org.neo4j.gds.scaling.ScalerFactory;

@Configuration("RankConfigImpl")
public interface RankConfig extends
    PregelConfig,
    ToleranceConfig,
    SourceNodesConfig
{
    @Override
    @Configuration.DoubleRange(min = 0D)
    default double tolerance() {
        return 1E-7;
    }

    @Override
    @Configuration.IntegerRange(min = 1)
    default int maxIterations() {
        return 20;
    }

    @Configuration.ConvertWith(method = "org.neo4j.gds.scaling.ScalerFactory#parse")
    @Configuration.ToMapValue("org.neo4j.gds.scaling.ScalerFactory#toString")
    default ScalerFactory scaler() {
        return NoneScaler.buildFrom(CypherMapWrapper.empty());
    }

    @Override
    @Configuration.Ignore
    default boolean isAsynchronous() {
        return false;
    }

    @Override
    @Configuration.Ignore
    default Partitioning partitioning() {
        return Partitioning.AUTO;
    }
}
