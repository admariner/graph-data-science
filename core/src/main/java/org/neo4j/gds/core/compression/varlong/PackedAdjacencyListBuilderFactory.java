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
package org.neo4j.gds.core.compression.varlong;

import org.neo4j.gds.api.compress.AdjacencyListBuilderFactory;
import org.neo4j.gds.core.compression.packed.PackedAdjacencyList;
import org.neo4j.gds.core.compression.packed.PackedAdjacencyListBuilder;
import org.neo4j.gds.core.compression.uncompressed.UncompressedAdjacencyList;
import org.neo4j.gds.core.compression.uncompressed.UncompressedAdjacencyListBuilder;

public final class PackedAdjacencyListBuilderFactory implements AdjacencyListBuilderFactory<Long, PackedAdjacencyList, long[], UncompressedAdjacencyList> {

    public static PackedAdjacencyListBuilderFactory of() {
        return new PackedAdjacencyListBuilderFactory();
    }

    private PackedAdjacencyListBuilderFactory() {
    }

    @Override
    public PackedAdjacencyListBuilder newAdjacencyListBuilder() {
        return new PackedAdjacencyListBuilder();
    }

    @Override
    public UncompressedAdjacencyListBuilder newAdjacencyPropertiesBuilder() {
        return new UncompressedAdjacencyListBuilder();
    }
}
