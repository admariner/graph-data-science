/*
 * Copyright (c) 2017-2020 "Neo4j,"
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
package org.neo4j.gds.ml.linkmodels.logisticregression;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinkFeatureCombinerTest {
    @Test
    void shouldBeCorrect() {
        double[] a = new double[] {5, 3.2, -4.2};
        double[] b = new double[] {-4.3, 7.2, 6.2};

        var combined = LinkFeatureCombiner.L2.combine(a, b);
        var expectedCombined = new double[]{(5 + 4.3) * (5 + 4.3), 4 * 4, (10.4) * (10.4)};
        assertThat(combined).containsExactly(expectedCombined);
    }

    @Test
    void shouldBeCorrect2() {
        double[] a = new double[] {5, 0, -4.2};
        double[] b = new double[] {5, 1, -4.2};

        var combined = LinkFeatureCombiner.L2.combine(a, b);
        var expectedCombined = new double[]{0, 1, 0};
        assertThat(combined).containsExactly(expectedCombined);
    }
}
