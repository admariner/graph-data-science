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
package org.neo4j.gds.core.loading.construction;

import org.neo4j.gds.api.PartialIdMap;
import org.neo4j.gds.api.compress.AdjacencyCompressor;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.utils.StringJoining;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongConsumer;

import static org.neo4j.gds.api.IdMap.NOT_FOUND;

public class RelationshipsBuilder {

    static final int NO_PROPERTY_REF = -1;

    private final PartialIdMap idMap;
    private final SingleTypeRelationshipsBuilder singleTypeRelationshipsBuilder;
    private final LocalRelationshipsBuilderProvider localBuilderProvider;
    private final boolean skipDanglingRelationships;

    RelationshipsBuilder(
        SingleTypeRelationshipsBuilder singleTypeRelationshipsBuilder,
        LocalRelationshipsBuilderProvider localBuilderProvider,
        boolean skipDanglingRelationships
    ) {
        this.singleTypeRelationshipsBuilder = singleTypeRelationshipsBuilder;
        this.idMap = singleTypeRelationshipsBuilder.partialIdMap();
        this.localBuilderProvider = localBuilderProvider;
        this.skipDanglingRelationships = skipDanglingRelationships;
    }

    public void add(long originalSourceId, long originalTargetId) {
        if (!addFromInternal(
            idMap.toMappedNodeId(originalSourceId),
            idMap.toMappedNodeId(originalTargetId)
        ) && !skipDanglingRelationships) {
            throwUnmappedNodeIds(originalSourceId, originalTargetId, idMap);
        }
    }

    public void add(long source, long target, double relationshipPropertyValue) {
        if (!addFromInternal(
            idMap.toMappedNodeId(source),
            idMap.toMappedNodeId(target),
            relationshipPropertyValue
        ) && !skipDanglingRelationships) {
            throwUnmappedNodeIds(source, target, idMap);
        }
    }

    public void add(long source, long target, double[] relationshipPropertyValues) {
        if (!addFromInternal(
            idMap.toMappedNodeId(source),
            idMap.toMappedNodeId(target),
            relationshipPropertyValues
        ) && !skipDanglingRelationships) {
            throwUnmappedNodeIds(source, target, idMap);
        }
    }

    public boolean addFromInternal(long mappedSourceId, long mappedTargetId) {
        if (validateRelationships(mappedSourceId, mappedTargetId)) {
            LocalRelationshipsBuilderProvider.LocalRelationshipsBuilderSlot threadLocalBuilder = null;
            try {
                threadLocalBuilder = localBuilderProvider.acquire();
                threadLocalBuilder.get().addRelationship(mappedSourceId, mappedTargetId);
            } finally {
                if (threadLocalBuilder != null) {
                    threadLocalBuilder.release();
                }
            }

            return true;
        }
        return false;
    }

    public boolean addFromInternal(long mappedSourceId, long mappedTargetId, double relationshipPropertyValue) {
        if (validateRelationships(mappedSourceId, mappedTargetId)) {
            LocalRelationshipsBuilderProvider.LocalRelationshipsBuilderSlot threadLocalBuilder = null;
            try {
                threadLocalBuilder = localBuilderProvider.acquire();
                threadLocalBuilder.get().addRelationship(mappedSourceId, mappedTargetId, relationshipPropertyValue);
            } finally {
                if (threadLocalBuilder != null) {
                    threadLocalBuilder.release();
                }
            }
            return true;
        }
        return false;
    }

    public boolean addFromInternal(long source, long target, double[] relationshipPropertyValues) {
        if (validateRelationships(source, target)) {
            LocalRelationshipsBuilderProvider.LocalRelationshipsBuilderSlot threadLocalBuilder = null;
            try {
                threadLocalBuilder = localBuilderProvider.acquire();
                threadLocalBuilder.get().addRelationship(source, target, relationshipPropertyValues);
            } finally {
                if (threadLocalBuilder != null) {
                    threadLocalBuilder.release();
                }
            }
            return true;
        }
        return false;
    }

    private boolean validateRelationships(long source, long target) {
        return source != NOT_FOUND && target != NOT_FOUND;
    }

    private static void throwUnmappedNodeIds(long source, long target, PartialIdMap idMap) {
        long mappedSource = idMap.toMappedNodeId(source);
        long mappedTarget = idMap.toMappedNodeId(target);

        var strings = new ArrayList<String>();
        if (mappedSource == NOT_FOUND) {
            strings.add(Long.toString(source));
        }
        if (mappedTarget == NOT_FOUND) {
            strings.add(Long.toString(target));
        }

        var message = String.format(
            Locale.US,
            "The following node ids are not present in the node id space: %s",
            StringJoining.join(strings)
        );

        throw new IllegalArgumentException(message);
    }

    public SingleTypeRelationships build() {
        return build(Optional.empty(), Optional.empty());
    }

    /**
     * @param mapper             A mapper to transform values before compressing them. Implementations must be thread-safe.
     * @param drainCountConsumer A consumer which is called once a {@link org.neo4j.gds.core.loading.ChunkedAdjacencyLists}
     *                           has been drained and its contents are written to the adjacency list. The consumer receives the number
     *                           of relationships that have been written. Implementations must be thread-safe.
     */
    public SingleTypeRelationships build(
        Optional<AdjacencyCompressor.ValueMapper> mapper,
        Optional<LongConsumer> drainCountConsumer
    ) {
        try {
            this.localBuilderProvider.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this.singleTypeRelationshipsBuilder.build(mapper, drainCountConsumer);
    }

    public interface Relationship {
        long sourceNodeId();

        long targetNodeId();

        double property();
    }
}
