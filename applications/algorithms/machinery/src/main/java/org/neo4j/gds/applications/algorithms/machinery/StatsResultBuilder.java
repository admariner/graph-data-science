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
package org.neo4j.gds.applications.algorithms.machinery;

import org.neo4j.gds.api.Graph;

import java.util.Optional;


public interface StatsResultBuilder<CONFIGURATION, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER> {
    /**
     * You implement this and use as much or as little of the gathered data as is appropriate.
     * Plus your own injected dependencies of course.
     *
     * @param result output from algorithm, empty when graph was empty
     */
    RESULT_TO_CALLER build(
        Graph graph,
        CONFIGURATION configuration,
        Optional<RESULT_FROM_ALGORITHM> result,
        AlgorithmProcessingTimings timings
    );
}