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
package org.saiku.query.metadata;

import org.olap4j.OlapException;
import org.olap4j.impl.Named;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.IdentifierSegment;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure.Aggregator;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CalculatedMember implements Member, Named, Calculated {
  private final Member parentMember;
  private Dimension dimension;
  private Hierarchy hierarchy;
  private String name;
  private String uniqueName;
  private Type memberType;
  private String formula;
  private Map<String, String> properties;
  private String description;
  private Level level;

  public CalculatedMember(Dimension dimension, Hierarchy hierarchy, String name, String description, Member parentMember, Type memberType, String formula, Map<String, String> properties, String l, boolean mondrian3) {
    this(dimension, hierarchy, name, description, parentMember, memberType, formula, properties, mondrian3);
    if(l != null && !l.equals("")) {
      Iterator i$ = hierarchy.getLevels().iterator();

      while(i$.hasNext()) {
	Level level = (Level)i$.next();
	if(level.getUniqueName().equals(l)) {
	  this.level = level;
	}
      }
    } else {
      this.level = (Level)hierarchy.getLevels().get(0);
    }

  }

  public CalculatedMember(Dimension dimension, Hierarchy hierarchy, String name, String description, Member parentMember, Type memberType, String formula, Map<String, String> properties, boolean mondrian3) {
    this.properties = new HashMap();
    this.dimension = dimension;
    this.hierarchy = hierarchy;
    this.level = (Level)hierarchy.getLevels().get(0);
    this.name = name;
    this.description = description;
    this.memberType = memberType;
    this.formula = formula;
    if(parentMember == null) {
      if(mondrian3) {
	this.uniqueName = IdentifierNode.ofNames(new String[]{hierarchy.getName(), name}).toString();
      } else {
	this.uniqueName = IdentifierNode.ofNames(new String[]{hierarchy.getDimension().getName(), hierarchy.getName(), name}).toString();
      }
    } else {
      IdentifierNode parent = IdentifierNode.parseIdentifier(parentMember.getUniqueName());
      IdentifierNode cm = IdentifierNode.ofNames(new String[]{name});
      ArrayList segmentList = new ArrayList();
      segmentList.addAll(parent.getSegmentList());
      segmentList.addAll(cm.getSegmentList());
      StringBuilder buf = new StringBuilder();

      IdentifierSegment segment;
      for(Iterator i$ = segmentList.iterator(); i$.hasNext(); buf.append(segment.toString())) {
	segment = (IdentifierSegment)i$.next();
	if(buf.length() > 0) {
	  buf.append('.');
	}
      }

      this.uniqueName = buf.toString();
    }

    this.parentMember = parentMember;
    if(properties != null) {
      this.properties.putAll(properties);
    }

  }

  public Dimension getDimension() {
    return this.dimension;
  }

  public Hierarchy getHierarchy() {
    return this.hierarchy;
  }

  public String getFormula() {
    return this.formula;
  }

  public Type getMemberType() {
    return this.memberType;
  }

  public Map<String, String> getFormatProperties() {
    return this.properties;
  }

  public String getFormatPropertyValue(String key) throws OlapException {
    return this.properties.containsKey(key)?(String)this.properties.get(key):null;
  }

  public void setFormatProperty(String key, String value) throws OlapException {
    this.properties.put(key, value);
  }

  public String getCaption() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getName() {
    return this.name;
  }

  public String getUniqueName() {
    return this.uniqueName;
  }

  public Aggregator getAggregator() {
    return Aggregator.CALCULATED;
  }

  public boolean isVisible() {
    return true;
  }

  public List<Member> getAncestorMembers() {
    throw new UnsupportedOperationException();
  }

  public int getChildMemberCount() throws OlapException {
    return 0;
  }

  public NamedList<? extends Member> getChildMembers() throws OlapException {
    throw new UnsupportedOperationException();
  }

  public Member getDataMember() {
    throw new UnsupportedOperationException();
  }

  public int getDepth() {
    return 0;
  }

  public ParseTreeNode getExpression() {
    throw new UnsupportedOperationException();
  }

  public Level getLevel() {
    return this.level;
  }

  public int getOrdinal() {
    throw new UnsupportedOperationException();
  }

  public Member getParentMember() {
    return this.parentMember;
  }

  public String getPropertyFormattedValue(Property property) throws OlapException {
    return String.valueOf(this.getPropertyValue(property));
  }

  public int getSolveOrder() {
    throw new UnsupportedOperationException();
  }

  public boolean isAll() {
    return false;
  }

  public boolean isCalculated() {
    return true;
  }

  public boolean isCalculatedInQuery() {
    return true;
  }

  public boolean isChildOrEqualTo(Member arg0) {
    return false;
  }

  public boolean isHidden() {
    return false;
  }

  /** @deprecated */
  @Deprecated
  public NamedList<Property> getProperties() {
    NamedListImpl l = new NamedListImpl(this.properties.entrySet());
    return l;
  }

  /** @deprecated */
  @Deprecated
  public Object getPropertyValue(Property p) throws OlapException {
    return this.properties.containsKey(p.getName())?this.properties.get(p.getName()):null;
  }

  /** @deprecated */
  @Deprecated
  public void setProperty(Property arg0, Object arg1) throws OlapException {
  }

  public int hashCode() {
    boolean prime = true;
    byte result = 1;
    int result1 = 31 * result + (this.uniqueName == null?0:this.uniqueName.hashCode());
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
      CalculatedMember other = (CalculatedMember)obj;
      if(this.uniqueName == null) {
	if(other.uniqueName != null) {
	  return false;
	}
      } else if(!this.uniqueName.equals(other.uniqueName)) {
	return false;
      }

      return true;
    }
  }
}
