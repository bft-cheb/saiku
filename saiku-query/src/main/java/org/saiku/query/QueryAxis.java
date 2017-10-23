/*  
 *   Copyright 2014 Paul Stoellberger
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.saiku.query;

import org.olap4j.Axis;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.metadata.NamedList;

import java.util.List;
import java.util.Locale;

public class QueryAxis extends AbstractSortableQuerySet {
    protected final NamedList<QueryHierarchy> hierarchies = new NamedListImpl();
    private final Query query;
    protected Axis location = null;
    private boolean nonEmpty;

    public QueryAxis(Query query, Axis location) {
        this.query = query;
        this.location = location;
    }

    public Axis getLocation() {
        return this.location;
    }

    public boolean isNonEmpty() {
        return this.nonEmpty;
    }

    public void setNonEmpty(boolean nonEmpty) {
        this.nonEmpty = nonEmpty;
    }

    public String getName() {
        return this.location.getCaption((Locale)null);
    }

    public Query getQuery() {
        return this.query;
    }

    public boolean isLowestLevelsOnly() {
        return this.query.isLowestLevelsOnly() | Axis.FILTER.equals(this.location);
    }

    public List<QueryHierarchy> getQueryHierarchies() {
        return this.hierarchies;
    }

    public void addHierarchy(QueryHierarchy hierarchy) {
        this.addHierarchy(-1, hierarchy);
    }

    public void addHierarchy(int index, QueryHierarchy hierarchy) {
        if(this.getQueryHierarchies().contains(hierarchy)) {
            throw new IllegalStateException("hierarchy already on this axis");
        } else {
            if(hierarchy.getAxis() != null && hierarchy.getAxis() != this) {
                hierarchy.getAxis().getQueryHierarchies().remove(hierarchy);
            }

            hierarchy.setAxis(this);
            if(index < this.hierarchies.size() && index >= 0) {
                this.hierarchies.add(index, hierarchy);
            } else {
                this.hierarchies.add(hierarchy);
            }

        }
    }

    public void removeHierarchy(QueryHierarchy hierarchy) {
        hierarchy.setAxis((QueryAxis)null);
        this.getQueryHierarchies().remove(hierarchy);
    }
}


