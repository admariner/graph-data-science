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
package org.neo4j.gds.ml.kge.scorers;

import com.carrotsearch.hppc.DoubleArrayList;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;

public class DoubleEuclideanDistanceLinkScorer implements LinkScorer {

    NodePropertyValues embeddings;

    double[] relationshipTypeEmbedding;

    long currentSourceNode;

    double[] currentCandidateTarget;

    DoubleEuclideanDistanceLinkScorer(NodePropertyValues embeddings, DoubleArrayList relationshipTypeEmbedding) {
        this.embeddings = embeddings;
        this.relationshipTypeEmbedding = relationshipTypeEmbedding.toArray();
    }

    @Override
    public void init(long sourceNode) {
        this.currentSourceNode = sourceNode;
        this.currentCandidateTarget = embeddings.doubleArrayValue(currentSourceNode);
        for(int i = 0; i < relationshipTypeEmbedding.length; i++){
            this.currentCandidateTarget[i] += relationshipTypeEmbedding[i];
        }
    }

    @Override
    public double computeScore(long targetNode) {
        double res = 0.0;
        var targetVector = embeddings.doubleArrayValue(targetNode);
        for (int i = 0; i < currentCandidateTarget.length; i++) {
            double elem = currentCandidateTarget[i] - targetVector[i];
            res += elem * elem;
        }
        return Math.sqrt(res);
    }

    @Override
    public void close() throws Exception { }

}