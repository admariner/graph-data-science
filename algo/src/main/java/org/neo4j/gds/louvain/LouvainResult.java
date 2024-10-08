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
package org.neo4j.gds.louvain;

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.collections.ha.HugeLongArray;

@SuppressWarnings("immutables:subtype")
public record LouvainResult(
        HugeLongArray communities,
        int ranLevels,
        @Nullable LouvainDendrogramManager dendrogramManager,
        double[] modularities,
        double modularity
    ){

    public  long[] intermediateCommunities(long nodeId) {

        if (dendrogramManager!= null) {
            var dendrograms = dendrogramManager.getAllDendrograms();
            int levels = ranLevels();
            long[] communities = new long[levels];
            for (int i = 0; i < levels; i++) {
                communities[i] = dendrograms[i].get(nodeId);
            }
            return communities;

        } else {
            return new long[]{communities.get(nodeId)};
        }

    }

    public long community(long node){
        return communities.get(node);
    }

    public long size() {
        return communities.size();
    }

}
