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
package org.neo4j.gds.ml.pipeline.linkPipeline.linkfunctions;

import org.neo4j.gds.ml.pipeline.FeatureStepUtil;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureAppender;

import java.util.ArrayList;
import java.util.Collection;

import static org.neo4j.gds.ml.pipeline.FeatureStepUtil.throwNanError;

class UnionLinkFeatureAppender implements LinkFeatureAppender {
    private final ArrayList<LinkFeatureAppender> appenderPerProperty;
    private final String featureStepName;
    private final Collection<String> inputNodeProperties;
    private final int dimension;

    public UnionLinkFeatureAppender(
        ArrayList<LinkFeatureAppender> appenderPerProperty,
        String featureStepName,
        Collection<String> inputNodeProperties
    ) {
        this.appenderPerProperty = appenderPerProperty;
        this.featureStepName = featureStepName;
        this.inputNodeProperties = inputNodeProperties;
        this.dimension = appenderPerProperty.stream().mapToInt(LinkFeatureAppender::dimension).sum();
    }

    @Override
    public void appendFeatures(long source, long target, double[] linkFeatures, int offset) {
        var localOffset = offset;

        for (LinkFeatureAppender linkFeatureAppender : appenderPerProperty) {
            linkFeatureAppender.appendFeatures(source, target, linkFeatures, localOffset);
            localOffset += linkFeatureAppender.dimension();
        }

        // TODO is this the right place to validate (rather expensive)
        FeatureStepUtil.validateComputedFeatures(linkFeatures, offset, localOffset, () -> throwNanError(featureStepName, inputNodeProperties, source, target)
        );
    }

    @Override
    public int dimension() {
        return dimension;
    }
}
