/*
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
import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.impl.IdentifierParser;
import org.olap4j.mdx.AxisNode;
import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.CubeNode;
import org.olap4j.mdx.LevelNode;
import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.SelectNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithMemberNode;
import org.olap4j.mdx.WithSetNode;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Level.Type;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.saiku.query.ISortableQuerySet.HierarchizeMode;
import org.saiku.query.Query.BackendFlavor;
import org.saiku.query.QueryDetails.Location;
import org.saiku.query.mdx.IFilterFunction;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Olap4jNodeConverter extends NodeConverter {
  public Olap4jNodeConverter() {
  }

  public static SelectNode toQuery(Query query) throws Exception {
    List cellpropertyList = Collections.emptyList();
    ArrayList withList = new ArrayList();
    ArrayList axisList = new ArrayList();
    Iterator filterAxis = query.getCalculatedMembers().iterator();

    while(filterAxis.hasNext()) {
      CalculatedMember axis = (CalculatedMember)filterAxis.next();
      WithMemberNode wm = toOlap4jCalculatedMember(axis);
      withList.add(wm);
    }

    axisList.add(query.getAxes().get(Axis.COLUMNS));
    axisList.add(query.getAxes().get(Axis.ROWS));
    AxisNode filterAxis1 = null;
    if(query.getAxes().containsKey(Axis.FILTER)) {
      QueryAxis axis1 = (QueryAxis)query.getAxes().get(Axis.FILTER);
      if(!axis1.hierarchies.isEmpty()) {
	filterAxis1 = toAxis(withList, axis1);
      }
    }

    return new SelectNode((ParseRegion)null, withList, toAxisList(withList, axisList), new CubeNode((ParseRegion)null, query.getCube()), filterAxis1, cellpropertyList);
  }

  private static List<AxisNode> toAxisList(List<ParseTreeNode> withList, List<QueryAxis> axes) throws Exception {
    ArrayList axisList = new ArrayList();
    Iterator i$ = axes.iterator();

    while(i$.hasNext()) {
      QueryAxis axis = (QueryAxis)i$.next();
      AxisNode axisNode = toAxis(withList, axis);
      if(axisNode != null) {
	axisList.add(axisNode);
      }
    }

    return axisList;
  }

  private static AxisNode toAxis(List<ParseTreeNode> withList, QueryAxis axis) throws Exception {
    BackendFlavor flavor = axis.getQuery().getFlavor();
    Object axisExpression = null;
    boolean axisAsSet = false;
    boolean isFilter = Axis.FILTER.equals(axis.getLocation());
    ArrayList axisNode;
    Iterator measuresNode;
    if(!axis.isMdxSetExpression()) {
      axisNode = new ArrayList();
      int details = axis.getQueryHierarchies().size();
      if(details == 1) {
	axisExpression = toHierarchy(withList, (QueryHierarchy)axis.getQueryHierarchies().get(0));
	axisAsSet = true;
      } else if(details > 1) {
	measuresNode = axis.getQueryHierarchies().iterator();

	while(measuresNode.hasNext()) {
	  QueryHierarchy axisNodes = (QueryHierarchy)measuresNode.next();
	  ParseTreeNode wm = toHierarchy(withList, axisNodes);
	  if(!isFilter) {
	    WithSetNode withNode = new WithSetNode((ParseRegion)null, getIdentifier(new String[]{axis.getName(), axisNodes.getHierarchy().getDimension().getName(), axisNodes.getName()}), wm);
	    withList.add(withNode);
	    axisNode.add(withNode.getIdentifier());
	  } else {
	    axisNode.add(wm);
	  }
	}

	if(System.getProperty("saiku.plugin") != null && System.getProperty("saiku.plugin").equals("true")) {
	  axisExpression = generateCrossJoin(axisNode, axis.isNonEmpty(), false);
	} else {
	  axisExpression = generateCrossJoin(axisNode, axis.isNonEmpty(), isFilter);
	}
      }
    }

    ParseTreeNode axisExpression1 = toSortedQuerySet((ParseTreeNode)axisExpression, axis);
    axisNode = null;
    Object axisNode1;
    if(flavor != null && !BackendFlavor.SSAS.equals(flavor) && axisExpression1 != null && axisAsSet) {
      WithSetNode details1 = new WithSetNode((ParseRegion)null, getIdentifier(new String[]{axis.getName()}), axisExpression1);
      withList.add(details1);
      axisNode1 = details1.getIdentifier();
    } else {
      axisNode1 = axisExpression1;
    }

    QueryDetails details2 = axis.getQuery().getDetails();
    if(details2.getMeasures().size() > 0 && axis.getLocation().equals(details2.getAxis())) {
      measuresNode = details2.getMeasures().iterator();

      while(measuresNode.hasNext()) {
	Measure axisNodes1 = (Measure)measuresNode.next();
	if(axisNodes1.isCalculatedInQuery()) {
	  WithMemberNode wm1 = toOlap4jCalculatedMember((CalculatedMeasure)axisNodes1);
	  withList.add(wm1);
	}
      }

      ParseTreeNode measuresNode1 = toOlap4jMeasureSet(details2.getMeasures());
      if(axisNode1 == null) {
	axisNode1 = measuresNode1;
      } else {
	ArrayList axisNodes2 = new ArrayList();
	if(details2.getLocation().equals(Location.TOP)) {
	  axisNodes2.add(measuresNode1);
	  axisNodes2.add(axisNode1);
	} else {
	  axisNodes2.add(axisNode1);
	  axisNodes2.add(measuresNode1);
	}

	axisNode1 = generateCrossJoin(axisNodes2);
      }
    }

    return axisNode1 == null?null:new AxisNode((ParseRegion)null, axis.isNonEmpty(), axis.getLocation(), new ArrayList(), (ParseTreeNode)axisNode1);
  }

  private static ParseTreeNode toHierarchy(List<ParseTreeNode> withList, QueryHierarchy h) throws OlapException {
    Object hierarchySet = null;
    if(!h.isMdxSetExpression()) {
      boolean isFilter = Axis.FILTER.equals(h.getAxis().getLocation());
      if(h.getActiveQueryLevels().size() == 0 && h.getActiveCalculatedMembers().size() == 0) {
	return new CallNode((ParseRegion)null, "{}", Syntax.Braces, new ArrayList());
      }

      ArrayList levels = new ArrayList();
      Object existSet = null;
      boolean allSimple = true;
      int firstComplex = -1;

      int levelSet;
      QueryLevel cmNodes;
      ParseTreeNode cmSet;
      for(levelSet = 0; levelSet < h.getActiveQueryLevels().size(); ++levelSet) {
	cmNodes = (QueryLevel)h.getActiveQueryLevels().get(levelSet);
	allSimple = allSimple & cmNodes != null & cmNodes.isSimple();
	if(!allSimple && firstComplex == -1) {
	  firstComplex = levelSet;
	  cmSet = toLevel(cmNodes);
	  toQuerySet(cmSet, cmNodes);
	  existSet = getIdentifier(new String[]{h.getHierarchy().getDimension().getName(), h.getName(), cmNodes.getName()});
	  break;
	}
      }

      for(levelSet = 0; levelSet < h.getActiveQueryLevels().size(); ++levelSet) {
	cmNodes = (QueryLevel)h.getActiveQueryLevels().get(levelSet);
	cmSet = toLevel(cmNodes);
	Object var16 = toQuerySet(cmSet, cmNodes);
	if(h.isConsistent() && existSet != null && levelSet != firstComplex && !cmNodes.getLevel().getLevelType().equals(Type.ALL)) {
	  var16 = new CallNode((ParseRegion)null, "Exists", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)var16, (ParseTreeNode)existSet});
	}

	if(!allSimple && h.getActiveQueryLevels().size() > 1) {
	  WithSetNode cm = new WithSetNode((ParseRegion)null, getIdentifier(new String[]{h.getHierarchy().getDimension().getName(), h.getName(), cmNodes.getName()}), (ParseTreeNode)var16);
	  withList.add(cm);
	  var16 = cm.getIdentifier();
	  if(!cmNodes.isSimple() || existSet != null && levelSet > firstComplex) {
	    existSet = var16;
	  }
	}

	levels.add(var16);
      }

      Object var13 = null;
      if(h.getAxis().isLowestLevelsOnly()) {
	ParseTreeNode var14 = (ParseTreeNode)levels.remove(levels.size() - 1);
	levels.clear();
	levels.add(var14);
      }

      if(levels.size() > 1) {
	var13 = generateListSetCall(levels);
      } else if(levels.size() == 1) {
	var13 = (ParseTreeNode)levels.get(0);
      }

      if(!isFilter && !h.getAxis().isLowestLevelsOnly() && h.needsHierarchize()) {
	var13 = new CallNode((ParseRegion)null, "Hierarchize", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)var13});
      }

      ArrayList var15 = new ArrayList();
      Iterator var18 = h.getActiveCalculatedMembers().iterator();

      while(var18.hasNext()) {
	CalculatedMember var17 = (CalculatedMember)var18.next();
	WithMemberNode wm = toOlap4jCalculatedMember(var17);
	var15.add(wm.getIdentifier());
      }

      if(var15.size() > 0) {
	CallNode var19 = generateListSetCall(var15);
	if(var13 != null) {
	  hierarchySet = generateSetCall(new ParseTreeNode[]{var19, (ParseTreeNode)var13});
	} else {
	  hierarchySet = var19;
	}
      } else {
	hierarchySet = var13;
      }
    }

    hierarchySet = toSortedQuerySet((ParseTreeNode)hierarchySet, h);
    if(h.isVisualTotals()) {
      if(h.getVisualTotalsPattern() != null) {
	hierarchySet = new CallNode((ParseRegion)null, "VisualTotals", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)hierarchySet, LiteralNode.createString((ParseRegion)null, h.getVisualTotalsPattern())});
      } else {
	hierarchySet = new CallNode((ParseRegion)null, "VisualTotals", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)hierarchySet});
      }
    }

    return (ParseTreeNode)hierarchySet;
  }

  private static ParseTreeNode toLevel(QueryLevel level) throws OlapException {
    ArrayList inclusions = new ArrayList();
    ArrayList exclusions = new ArrayList();
    inclusions.addAll(level.getInclusions());
    exclusions.addAll(level.getExclusions());
    String exceptSet;
    if(level.hasParameter()) {
      String baseNode = level.getParameterName();
      exceptSet = level.getQueryHierarchy().getQuery().getParameter(baseNode);
      if(StringUtils.isNotBlank(exceptSet)) {
	List endExpr = resolveParameter(level.getQueryHierarchy().getQuery().getCube(), level.getUniqueName(), exceptSet);
	switch(level.getParameterSelectionType()) {
          case EXCLUSION:
	    exclusions.clear();
	    exclusions.addAll(endExpr);
	    break;
          case INCLUSION:
	    inclusions.clear();
	    inclusions.addAll(endExpr);
	}
      }
    }

    Object baseNode1 = new CallNode((ParseRegion)null, "Members", Syntax.Property, new ParseTreeNode[]{new LevelNode((ParseRegion)null, level.getLevel())});
    if(level.getLevel().getLevelType().equals(Type.ALL)) {
      try {
	baseNode1 = new MemberNode((ParseRegion)null, level.getLevel().getHierarchy().getDefaultMember());
      } catch (OlapException var6) {
	throw new RuntimeException("Cannot include hierarchy default member for " + level.getUniqueName());
      }
    }

    baseNode1 = generateSetCall(new ParseTreeNode[]{(ParseTreeNode)baseNode1});
    if(level.isRange()) {
      if(level.getRangeStart() != null && level.getRangeEnd() != null) {
	ArrayList exceptSet1 = new ArrayList();
	exceptSet1.add(new MemberNode((ParseRegion)null, level.getRangeStart()));
	exceptSet1.add(new MemberNode((ParseRegion)null, level.getRangeEnd()));
	baseNode1 = new CallNode((ParseRegion)null, ":", Syntax.Infix, exceptSet1);
      } else {
	exceptSet = level.getRangeStartExpr();
	String endExpr1 = level.getRangeEndExpr();
	if(StringUtils.isBlank(endExpr1)) {
	  baseNode1 = generateSetCall(new ParseTreeNode[]{toMdxNode(exceptSet)});
	} else {
	  baseNode1 = generateSetCall(new ParseTreeNode[]{toMdxNode(exceptSet + " : " + endExpr1)});
	}
      }
    }

    if(inclusions.size() > 0) {
      baseNode1 = toOlap4jMemberSet(inclusions);
    }

    if(exclusions.size() > 0) {
      ParseTreeNode exceptSet2 = toOlap4jMemberSet(exclusions);
      baseNode1 = new CallNode((ParseRegion)null, "Except", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)baseNode1, exceptSet2});
    }

    return (ParseTreeNode)baseNode1;
  }

  /** @deprecated */
  @Deprecated
  private static List<Member> resolveParameter(Cube cube, String parent, String value) throws OlapException {
    List parentParts = null;
    if(StringUtils.isNotBlank(parent)) {
      parentParts = IdentifierParser.parseIdentifier(parent);
    }

    ArrayList resolvedList = new ArrayList();
    if(StringUtils.isNotBlank(value)) {
      String[] vs = value.split(",");
      String[] arr$ = vs;
      int len$ = vs.length;

      for(int i$ = 0; i$ < len$; ++i$) {
	String v = arr$[i$];
	v = v.trim();
	if(StringUtils.isNotBlank(v)) {
	  List nameParts = IdentifierParser.parseIdentifier(v);
	  ArrayList combined = new ArrayList();
	  if(parentParts != null && nameParts.size() == 1) {
	    combined.addAll(parentParts);
	  }

	  combined.addAll(nameParts);
	  Member m = cube.lookupMember(combined);
	  if(m == null) {
	    throw new OlapException("Cannot find member with name parts: " + combined.toString());
	  }

	  resolvedList.add(m);
	}
      }
    }

    return resolvedList;
  }

  private static ParseTreeNode toMdxNode(String mdx) {
    DefaultMdxParserImpl parser = new DefaultMdxParserImpl();
    ParseTreeNode expression = parser.parseExpression(mdx);
    return expression;
  }

  private static ParseTreeNode toQuerySet(ParseTreeNode expression, IQuerySet o) {
    DefaultMdxParserImpl parser = new DefaultMdxParserImpl();
    if(o.isMdxSetExpression()) {
      expression = toMdxNode("{" + o.getMdxSetExpression() + "}");
    }

    IFilterFunction filter;
    if(expression != null && o.getFilters().size() > 0) {
      for(Iterator i$ = o.getFilters().iterator(); i$.hasNext(); expression = filter.visit(parser, expression)) {
	filter = (IFilterFunction)i$.next();
      }
    }

    return expression;
  }

  private static ParseTreeNode toSortedQuerySet(ParseTreeNode expression, ISortableQuerySet o) {
    Object expression1 = toQuerySet(expression, o);
    if(expression1 == null) {
      return null;
    } else {
      if(o.getSortOrder() != null) {
	LiteralNode evaluatorNode = LiteralNode.createSymbol((ParseRegion)null, o.getSortEvaluationLiteral());
	expression1 = new CallNode((ParseRegion)null, "Order", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)expression1, evaluatorNode, LiteralNode.createSymbol((ParseRegion)null, o.getSortOrder().name())});
      } else if(o.getHierarchizeMode() != null) {
	if(o.getHierarchizeMode().equals(HierarchizeMode.PRE)) {
	  expression1 = new CallNode((ParseRegion)null, "Hierarchize", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)expression1});
	} else {
	  if(!o.getHierarchizeMode().equals(HierarchizeMode.POST)) {
	    throw new RuntimeException("Missing value handler.");
	  }

	  expression1 = new CallNode((ParseRegion)null, "Hierarchize", Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)expression1, LiteralNode.createSymbol((ParseRegion)null, o.getHierarchizeMode().name())});
	}
      }

      return (ParseTreeNode)expression1;
    }
  }
}










