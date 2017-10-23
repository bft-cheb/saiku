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
import org.olap4j.impl.Named;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Level.Type;
import org.olap4j.metadata.Member;
import org.saiku.query.Parameter.SelectionType;

import java.util.ArrayList;
import java.util.List;

public class QueryLevel extends AbstractQuerySet implements Named {
  private final QueryHierarchy hierarchy;
  private final Level level;
  private List<Member> inclusions = new ArrayList();
  private List<Member> exclusions = new ArrayList();
  private Member rangeStart = null;
  private Member rangeEnd = null;
  private String rangeStartExpr = null;
  private String rangeEndExpr = null;
  private String rangeStartSyn;
  private String rangeEndSyn;
  private String parameterName = null;
  private SelectionType parameterSelectionType;

  public QueryLevel(QueryHierarchy hierarchy, Level level) {
    this.parameterSelectionType = SelectionType.INCLUSION;
    this.hierarchy = hierarchy;
    this.level = level;
  }

  public QueryHierarchy getQueryHierarchy() {
    return this.hierarchy;
  }

  public String getName() {
    return this.level.getName();
  }

  public String getUniqueName() {
    return this.level.getUniqueName();
  }

  public String getCaption() {
    return this.level.getCaption();
  }

  public boolean isSimple() {
    return super.isSimple() && (this.level.getLevelType().equals(Type.ALL) || this.inclusions.isEmpty() && this.exclusions.isEmpty() && this.rangeStart == null && this.rangeEnd == null && this.rangeStartExpr == null && this.rangeEndExpr == null) && (!this.hasParameter() || this.hierarchy.getQuery().getParameter(this.getParameterName()) == null);
  }

  public boolean isRange() {
    return this.rangeStart != null && this.rangeEnd != null || this.rangeStartExpr != null || this.rangeEndExpr != null;
  }

  public Level getLevel() {
    return this.level;
  }

  public List<Member> getInclusions() {
    return this.inclusions;
  }

  public List<Member> getExclusions() {
    return this.exclusions;
  }

  public Member getRangeStart() {
    return this.rangeStart;
  }

  public Member getRangeEnd() {
    return this.rangeEnd;
  }

  public String getRangeStartExpr() {
    return this.rangeStartExpr;
  }

  public String getRangeEndExpr() {
    return this.rangeEndExpr;
  }

  public String getRangeStartSyn() {
    return this.rangeStartSyn;
  }

  public String getRangeEndSyn() {
    return this.rangeEndSyn;
  }

  protected void clearSelections() {
    this.inclusions.clear();
    this.exclusions.clear();
    this.rangeStart = null;
    this.rangeStartExpr = null;
    this.rangeStartSyn = null;
    this.rangeEnd = null;
    this.rangeEndExpr = null;
    this.rangeEndSyn = null;
  }

  protected void include(Member m) {
    if(!this.inclusions.contains(m)) {
      this.inclusions.add(m);
    }

  }

  protected void exclude(Member m) {
    if(this.inclusions.contains(m)) {
      this.inclusions.remove(m);
    }

    if(!this.exclusions.contains(m)) {
      this.exclusions.add(m);
    }

  }

  protected void setRange(Member start, Member end) {
    this.rangeStart = start;
    this.rangeEnd = end;
  }

  public void setRangeSynonyms(String startSynonym, String endSynonym) {
    this.rangeStartSyn = startSynonym;
    this.rangeEndSyn = endSynonym;
  }

  public void setRangeStartSynonym(String startSyn) {
    this.rangeStartSyn = startSyn;
  }

  public void setRangeEndSynonym(String endSyn) {
    this.rangeEndSyn = endSyn;
  }

  public void setRangeStartExpr(String startExp) {
    this.rangeStart = null;
    this.rangeStartExpr = startExp;
  }

  public void setRangeEndExpr(String endExp) {
    this.rangeEnd = null;
    this.rangeEndExpr = endExp;
  }

  public void setRangeExpressions(String startExpr, String endExpr) {
    this.rangeStart = null;
    this.rangeEnd = null;
    this.rangeStartExpr = startExpr;
    this.rangeEndExpr = endExpr;
  }

  public int hashCode() {
    boolean prime = true;
    int result = super.hashCode();
    result = 31 * result + (this.level == null?0:this.level.getUniqueName().hashCode());
    return result;
  }

  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    } else if(!super.equals(obj)) {
      return false;
    } else if(this.getClass() != obj.getClass()) {
      return false;
    } else {
      QueryLevel other = (QueryLevel)obj;
      if(this.level == null) {
	if(other.level != null) {
	  return false;
	}
      } else if(!this.level.getUniqueName().equals(other.getLevel().getUniqueName())) {
	return false;
      }

      return true;
    }
  }

  public String toString() {
    return this.level.getUniqueName();
  }

  public void setParameterName(String parameter) {
    this.parameterName = parameter;
  }

  public void setParameterSelectionType(SelectionType selectionType) {
    this.parameterSelectionType = selectionType;
  }

  public String getParameterName() {
    return this.parameterName;
  }

  public SelectionType getParameterSelectionType() {
    return this.parameterSelectionType;
  }

  public boolean hasParameter() {
    return StringUtils.isNotBlank(this.parameterName);
  }
}








