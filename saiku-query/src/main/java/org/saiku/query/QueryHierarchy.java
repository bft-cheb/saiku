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

import org.olap4j.OlapException;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.impl.Named;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.saiku.query.metadata.CalculatedMember;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class QueryHierarchy extends AbstractSortableQuerySet implements Named {
  private final NamedList<RootMember> rootMembers = new NamedListImpl();
  protected QueryAxis axis;
  private final Query query;
  private final Hierarchy hierarchy;
  private NamedList<QueryLevel> queryLevels = new NamedListImpl();
  private NamedList<QueryLevel> activeLevels = new NamedListImpl();
  private NamedList<CalculatedMember> calculatedMembers = new NamedListImpl();
  private NamedList<CalculatedMember> activeCalculatedMembers = new NamedListImpl();
  private boolean consistent = true;
  private boolean visualTotals = false;
  private String visualTotalsPattern;

  public QueryHierarchy(Query query, Hierarchy hierarchy) {
    this.query = query;
    this.hierarchy = hierarchy;
    Iterator e = hierarchy.getLevels().iterator();

    while(e.hasNext()) {
      Level i$ = (Level)e.next();
      QueryLevel member = new QueryLevel(this, i$);
      this.queryLevels.add(member);
    }

    try {
      NamedList e1 = hierarchy.getRootMembers();
      Iterator i$1 = e1.iterator();

      while(i$1.hasNext()) {
	Member member1 = (Member)i$1.next();
	RootMember rootMember = new RootMember(this, member1);
	this.rootMembers.add(rootMember);
      }
    } catch (OlapException var7) {
      var7.printStackTrace();
    }

  }

  public Query getQuery() {
    return this.query;
  }

  public QueryAxis getAxis() {
    return this.axis;
  }

  protected void setAxis(QueryAxis axis) {
    this.axis = axis;
  }

  public String getName() {
    return this.hierarchy.getName();
  }

  public String getUniqueName() {
    return this.hierarchy.getUniqueName();
  }

  public String getCaption() {
    return this.hierarchy.getCaption();
  }

  public boolean isConsistent() {
    return this.consistent;
  }

  public void setConsistent(boolean consistent) {
    this.consistent = consistent;
  }

  public boolean isVisualTotals() {
    return this.visualTotals | this.query.isVisualTotals();
  }

  public void setVisualTotals(boolean visualTotals) {
    this.visualTotals = visualTotals;
    if(!visualTotals) {
      this.visualTotalsPattern = null;
    }

  }

  public void setVisualTotalsPattern(String pattern) {
    this.visualTotalsPattern = pattern;
    this.visualTotals = true;
  }

  public String getVisualTotalsPattern() {
    return this.visualTotalsPattern == null?this.query.getVisualTotalsPattern():this.visualTotalsPattern;
  }

  public boolean needsHierarchize() {
    return this.visualTotals | this.activeLevels.size() > 1 && this.getHierarchizeMode() == null;
  }

  public Hierarchy getHierarchy() {
    return this.hierarchy;
  }

  public void addCalculatedMember(CalculatedMember cm) {
    this.calculatedMembers.add(cm);
  }

  public NamedList<CalculatedMember> getCalculatedMembers() {
    return this.calculatedMembers;
  }

  public List<CalculatedMember> getActiveCalculatedMembers() {
    return this.activeCalculatedMembers;
  }

  public List<QueryLevel> getActiveQueryLevels() {
    Collections.sort(this.activeLevels, new QueryHierarchy.SaikuQueryLevelComparator());
    return this.activeLevels;
  }

  public QueryLevel getActiveLevel(String levelName) {
    return (QueryLevel)this.activeLevels.get(levelName);
  }

  public QueryLevel includeLevel(String levelName) {
    QueryLevel ql = (QueryLevel)this.queryLevels.get(levelName);
    if(ql != null && !this.activeLevels.contains(ql)) {
      this.activeLevels.add(ql);
    }

    return ql;
  }

  public QueryLevel includeMemberLevel(String levelName) {
    QueryLevel ql = (QueryLevel)this.queryLevels.get(levelName);
    if(ql != null && !this.activeLevels.contains(ql)) {
      this.activeLevels.add(ql);
    }

    return ql;
  }

  public QueryLevel includeLevel(Level l) throws OlapException {
    if(!l.getHierarchy().equals(this.hierarchy)) {
      throw new OlapException("You cannot include level " + l.getUniqueName() + " on hierarchy " + this.hierarchy.getUniqueName());
    } else {
      QueryLevel ql = (QueryLevel)this.queryLevels.get(l.getName());
      if(ql != null && !this.activeLevels.contains(l)) {
	this.activeLevels.add(ql);
      }

      return ql;
    }
  }

  public void excludeLevel(String levelName) {
    QueryLevel ql = (QueryLevel)this.queryLevels.get(levelName);
    if(ql != null && this.activeLevels.contains(ql)) {
      this.activeLevels.remove(ql);
    }

  }

  public void excludeLevel(Level l) throws OlapException {
    QueryLevel ql = (QueryLevel)this.queryLevels.get(l.getName());
    if(ql != null && !this.activeLevels.contains(l)) {
      this.activeLevels.remove(ql);
    }

  }

  public void includeMembers(List<Member> members) throws OlapException {
    Iterator i$ = members.iterator();

    while(i$.hasNext()) {
      Member m = (Member)i$.next();
      this.includeMember(m);
    }

  }

  public void includeMember(String uniqueMemberName) throws OlapException {
    List nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    this.includeMember(nameParts);
  }

  public void includeMember(List<IdentifierSegment> nameParts) throws OlapException {
    Member member = this.query.getCube().lookupMember(nameParts);
    if(member == null) {
      throw new OlapException("Unable to find a member with name " + nameParts);
    } else {
      this.includeMember(member);
    }
  }

  public void includeCalculatedMember(CalculatedMember m, boolean include) throws OlapException {
    Hierarchy h = m.getHierarchy();
    if(!h.equals(this.hierarchy)) {
      throw new OlapException("You cannot include the calculated member " + m.getUniqueName() + " on hierarchy " + this.hierarchy.getUniqueName());
    } else {
      if(!this.calculatedMembers.contains(m)) {
	this.calculatedMembers.add(m);
      }

      if(include) {
	QueryLevel ql = (QueryLevel)this.queryLevels.get(m.getLevel().getName());
	ql.include(m);
      } else {
	this.activeCalculatedMembers.add(m);
      }

    }
  }

  public void excludeCalculatedMember(CalculatedMember m) throws OlapException {
    this.activeCalculatedMembers.remove(m);
  }

  public void includeMember(Member m) throws OlapException {
    Level l = m.getLevel();
    if(!l.getHierarchy().equals(this.hierarchy)) {
      throw new OlapException("You cannot include member " + m.getUniqueName() + " on hierarchy " + this.hierarchy.getUniqueName());
    } else {
      QueryLevel ql = (QueryLevel)this.queryLevels.get(l.getName());
      if(!this.activeLevels.contains(ql)) {
	this.activeLevels.add(ql);
      }

      ql.include(m);
    }
  }

  public void includeRange(String uniqueMemberNameStart, String uniqueMemberNameEnd) throws OlapException {
    List namePartsStart = IdentifierParser.parseIdentifier(uniqueMemberNameStart);
    List namePartsEnd = IdentifierParser.parseIdentifier(uniqueMemberNameEnd);
    this.includeRange(namePartsStart, namePartsEnd);
  }

  public void includeRange(List<IdentifierSegment> namePartsStart, List<IdentifierSegment> namePartsEnd) throws OlapException {
    Member rangeStart = this.query.getCube().lookupMember(namePartsStart);
    Member rangeEnd = this.query.getCube().lookupMember(namePartsEnd);
    if(rangeStart == null) {
      throw new OlapException("Unable to find a member with name " + rangeStart);
    } else if(rangeEnd == null) {
      throw new OlapException("Unable to find a member with name " + rangeEnd);
    } else {
      this.includeRange(rangeStart, rangeEnd);
    }
  }

  public void includeRange(Member start, Member end) throws OlapException {
    Level l = start.getLevel();
    if(!start.getLevel().equals(end.getLevel())) {
      throw new OlapException("A range selection must include members from the same level (" + start.getLevel().getName() + " vs. " + end.getLevel().getName());
    } else if(!l.getHierarchy().equals(this.hierarchy)) {
      throw new OlapException("Hierarchy not matching. You cannot include a range selection for " + start.getUniqueName() + " and " + end.getUniqueName() + " on hierarchy " + this.hierarchy.getUniqueName());
    } else {
      QueryLevel ql = (QueryLevel)this.queryLevels.get(l.getName());
      if(!this.activeLevels.contains(ql)) {
	this.activeLevels.add(ql);
      }

      ql.setRange(start, end);
    }
  }

  public void excludeMember(String uniqueMemberName) throws OlapException {
    List nameParts = IdentifierParser.parseIdentifier(uniqueMemberName);
    this.excludeMember(nameParts);
  }

  public void excludeMember(List<IdentifierSegment> nameParts) throws OlapException {
    Member member = this.query.getCube().lookupMember(nameParts);
    if(member == null) {
      throw new OlapException("Unable to find a member with name " + nameParts);
    } else {
      this.excludeMember(member);
    }
  }

  public void excludeMembers(List<Member> members) {
    Iterator i$ = members.iterator();

    while(i$.hasNext()) {
      Member m = (Member)i$.next();
      this.excludeMember(m);
    }

  }

  public void excludeMember(Member m) {
    Level l = m.getLevel();
    if(!l.getHierarchy().equals(this.hierarchy)) {
      throw new IllegalArgumentException("You cannot exclude member " + m.getUniqueName() + " on hierarchy " + this.hierarchy.getUniqueName());
    } else {
      QueryLevel ql = (QueryLevel)this.queryLevels.get(l.getName());
      if(!this.activeLevels.contains(ql)) {
	this.activeLevels.add(ql);
      }

      ql.exclude(m);
    }
  }

  public void clearSelection() {
    if(this.activeLevels != null) {
      Iterator i$ = this.activeLevels.iterator();

      while(i$.hasNext()) {
	QueryLevel ql = (QueryLevel)i$.next();
	ql.clearSelections();
      }
    }

    this.activeLevels.clear();
  }

  public int hashCode() {
    boolean prime = true;
    int result = super.hashCode();
    result = 31 * result + (this.hierarchy == null?0:this.hierarchy.getUniqueName().hashCode());
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
      QueryHierarchy other = (QueryHierarchy)obj;
      if(this.hierarchy == null) {
	if(other.hierarchy != null) {
	  return false;
	}
      } else if(!this.hierarchy.getUniqueName().equals(other.hierarchy.getUniqueName())) {
	return false;
      }

      return true;
    }
  }

  public String toString() {
    return this.hierarchy.getUniqueName();
  }

  private class SaikuQueryLevelComparator implements Comparator<QueryLevel> {
    private SaikuQueryLevelComparator() {
    }

    public int compare(QueryLevel o1, QueryLevel o2) {
      return o1 != null && o2 != null?o1.getLevel().getDepth() - o2.getLevel().getDepth():0;
    }
  }
}








