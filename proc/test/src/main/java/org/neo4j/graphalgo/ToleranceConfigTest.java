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
package org.neo4j.graphalgo;

import org.junit.jupiter.api.Test;
import org.neo4j.graphalgo.compat.MapUtil;
import org.neo4j.graphalgo.config.AlgoBaseConfig;
import org.neo4j.graphalgo.config.ToleranceConfig;
import org.neo4j.graphalgo.core.CypherMapWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface ToleranceConfigTest <CONFIG extends ToleranceConfig & AlgoBaseConfig, RESULT> extends AlgoBaseProcTest<CONFIG, RESULT> {
    @Test
    default void testToleranceFromConfig() {
        CypherMapWrapper mapWrapper = CypherMapWrapper.create(MapUtil.map("tolerance", 42.42));
        CONFIG config = createConfig(createMinimalConfig(mapWrapper));
        assertEquals(42.42, config.tolerance());
    }
}
