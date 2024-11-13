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
package org.neo4j.gds.indirectExposure;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.core.CypherMapWrapper;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class IndirectExposureMutateConfigTest {

    @Test
    void create() {
        Map<String, Object> map = Map.of(
            "sanctionedProperty", "sanctioned",
            "maxIterations", 10,
            "relationshipWeightProperty", "w",
            "mutateProperties", Map.of(
                "exposures", "exposures",
                "hops", "hops",
                "parents", "parents",
                "roots", "roots"
            )
        );

        var wrapper = CypherMapWrapper.create(map);
        var config = IndirectExposureMutateConfig.of(wrapper);

        assertThat(config.sanctionedProperty()).isEqualTo("sanctioned");
        assertThat(config.mutateProperties().exposures()).isEqualTo("exposures");
        assertThat(config.mutateProperties().hops()).isEqualTo("hops");
        assertThat(config.mutateProperties().parents()).isEqualTo("parents");
        assertThat(config.mutateProperties().roots()).isEqualTo("roots");
    }
}