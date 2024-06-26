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
package org.neo4j.gds.collections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface HugeAtomicArray {

    /**
     * The type which is stored in the generated HugeAtomicArray.
     */
    Class<?> valueType();

    /**
     * The functional interface which takes the valueType and returns the valueType.
     * The method is expected to be named "apply".
     */
    Class<?> valueOperatorInterface();

    /**
     * The interface which is expected to extend PageCreator.
     */
    Class<?> pageCreatorInterface();

    /**
     * The page shift defines the page size used in the
     * generated HugeAtomicArray. The default value of 14
     * leads so 2^14 = 16384 elements per page (array).
     */
    int pageShift() default 14;
}
