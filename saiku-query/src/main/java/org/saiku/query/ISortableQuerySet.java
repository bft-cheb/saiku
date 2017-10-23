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

public interface ISortableQuerySet extends IQuerySet {
    void sort(SortOrder var1);

    void sort(SortOrder var1, String var2);

    SortOrder getSortOrder();

    String getSortEvaluationLiteral();

    void clearSort();

    ISortableQuerySet.HierarchizeMode getHierarchizeMode();

    void setHierarchizeMode(ISortableQuerySet.HierarchizeMode var1);

    void clearHierarchizeMode();

    public static enum HierarchizeMode {
        PRE,
        POST;

        private HierarchizeMode() {
        }
    }
}
