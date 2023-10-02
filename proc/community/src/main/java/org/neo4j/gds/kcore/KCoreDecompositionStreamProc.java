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
package org.neo4j.gds.kcore;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.procedures.GraphDataScience;
import org.neo4j.gds.procedures.community.kcore.KCoreStreamResult;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.kcore.KCoreDecomposition.KCORE_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class KCoreDecompositionStreamProc extends BaseProc {
    @Context
    public GraphDataScience facade;

    @Procedure(value = "gds.kcore.stream", mode = READ)
    @Description(KCORE_DESCRIPTION)
    public Stream<KCoreStreamResult> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.community().kCoreStream(graphName, configuration);
    }
    @Procedure(value = "gds.kcore.stream.estimate", mode = READ)
    @Description(KCORE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return facade.community().kCoreEstimateStream(graphNameOrConfiguration, algoConfiguration);

    }

}
