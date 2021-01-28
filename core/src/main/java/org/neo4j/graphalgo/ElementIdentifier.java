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
package org.neo4j.graphalgo;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ElementIdentifier {

    public final @NotNull String name;

    public ElementIdentifier(@NotNull String name) {
//        TODO: We need to disable this until we have found a solution for handling * vs empty String
//        if (StringUtils.isEmpty(name)) {
//            throw new IllegalArgumentException("name cannot be empty");
//        }
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public abstract ElementIdentifier projectAll();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementIdentifier that = (ElementIdentifier) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return String.format("%s{" +
               "name='" + name + '\'' +
               '}', this.getClass().getSimpleName());
    }
}
