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

import org.apache.commons.lang.StringUtils;
import org.saiku.query.mdx.IFilterFunction;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractQuerySet implements IQuerySet {
  private String mdxExpression;
  private List<IFilterFunction> filters = new ArrayList();

  public AbstractQuerySet() {
  }

  public abstract String getName();

  public boolean isSimple() {
    return this.mdxExpression == null && this.filters.isEmpty();
  }

  public void setMdxSetExpression(String mdxSetExpression) {
    this.mdxExpression = mdxSetExpression;
  }

  public String getMdxSetExpression() {
    return this.mdxExpression;
  }

  public boolean isMdxSetExpression() {
    return this.mdxExpression != null;
  }

  public void addFilter(IFilterFunction filter) {
    this.filters.add(filter);
  }

  public void setFilter(int index, IFilterFunction filter) {
    this.filters.set(index, filter);
  }

  public List<IFilterFunction> getFilters() {
    return this.filters;
  }

  public void clearFilters() {
    this.filters.clear();
  }

  public int hashCode() {
    boolean prime = true;
    byte result = 1;
    int result1 = 31 * result + (this.mdxExpression == null?0:this.mdxExpression.hashCode());
    return result1;
  }

  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    } else if(obj == null) {
      return false;
    } else if(this.getClass() != obj.getClass()) {
      return false;
    } else {
      AbstractQuerySet other = (AbstractQuerySet)obj;
      if(this.mdxExpression == null) {
	if(other.mdxExpression != null) {
	  return false;
	}
      } else if(!this.mdxExpression.equals(other.mdxExpression)) {
	return false;
      }

      return StringUtils.equals(this.getName(), other.getName());
    }
  }
}
