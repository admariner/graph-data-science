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
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.config.ArrowConnectionInfo;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.logging.Log;

import java.util.Optional;

public class WriteNodePropertyService {
    private final Log log;
    private final RequestScopedDependencies requestScopedDependencies;
    private final WriteContext writeContext;

    public WriteNodePropertyService(Log log, RequestScopedDependencies requestScopedDependencies, WriteContext writeContext) {
        this.log = log;
        this.requestScopedDependencies = requestScopedDependencies;
        this.writeContext = writeContext;
    }

    public WriteNodePropertyResult write(
        Graph graph,
        GraphStore graphStore,
        NodePropertyValues nodePropertyValues,
        Concurrency writeConcurrency,
        String writeProperty,
        String procedureName,
        Optional<ArrowConnectionInfo> arrowConnectionInfo,
        Optional<ResultStore> resultStore,
        JobId jobId
    ) {
        return Neo4jDatabaseNodePropertyWriter.writeNodeProperty(
            writeContext.getNodePropertyExporterBuilder(),
            requestScopedDependencies.getTaskRegistryFactory(),
            graph,
            graphStore,
            nodePropertyValues,
            writeConcurrency,
            writeProperty,
            procedureName,
            arrowConnectionInfo,
            resultStore,
            jobId,
            requestScopedDependencies.getTerminationFlag(),
            log
        );
    }
}
