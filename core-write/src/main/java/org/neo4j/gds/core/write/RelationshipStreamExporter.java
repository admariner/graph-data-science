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
package org.neo4j.gds.core.write;

import org.jetbrains.annotations.TestOnly;
import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface RelationshipStreamExporter {

    @TestOnly
    default long write(String relationshipType, String... propertyKeys) {
        return write(
            relationshipType,
            Arrays.stream(propertyKeys).collect(Collectors.toList()),
            new ArrayList<>(Collections.nCopies( propertyKeys.length, ValueType.DOUBLE)));
    }

    /**
     * @param propertyKeys - keys of the properties to write
     * @param propertyTypes - types of the properties, corresponding to the keys
     */
    long write(String relationshipType, List<String> propertyKeys, List<ValueType> propertyTypes);

    static Task baseTask(String operationName) {
        return Tasks.leaf(operationName + " :: WriteRelationshipStream");
    }
}
