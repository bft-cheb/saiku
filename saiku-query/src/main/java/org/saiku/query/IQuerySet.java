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

import org.saiku.query.mdx.IFilterFunction;

import java.util.List;

public interface IQuerySet {
    String getName();

    boolean isSimple();

    void setMdxSetExpression(String var1);

    String getMdxSetExpression();

    boolean isMdxSetExpression();

    void addFilter(IFilterFunction var1);

    void setFilter(int var1, IFilterFunction var2);

    List<IFilterFunction> getFilters();

    void clearFilters();
}
