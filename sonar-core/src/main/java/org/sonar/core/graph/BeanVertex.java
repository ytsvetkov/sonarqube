/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.graph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public abstract class BeanVertex extends BeanElement<Vertex, BeanVertex> {

  protected final <T extends BeanEdge> Iterable<T> getEdges(Class<T> edgeClass, Direction direction, String... labels) {
    return new BeanIterable<T>(beanGraph(), edgeClass, element().getEdges(direction, labels));
  }

  protected final <T extends BeanVertex> Iterable<T> getVertices(Class<T> vertexClass, Direction direction, String... labels) {
    return new BeanIterable<T>(beanGraph(), vertexClass, element().getVertices(direction, labels));
  }
}
