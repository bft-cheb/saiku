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
package org.saiku.query.mdx;

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.HierarchyNode;
import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.parser.MdxParser;
import org.olap4j.metadata.Hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NameFilter extends AbstractFilterFunction {
  private String operator = " = ";
  private String op;
  private List<String> filterExpression = new ArrayList();
  private MdxFunctionType type;
  private Hierarchy hierarchy;

  public NameFilter(Hierarchy hierarchy, String... matchingExpression) {
    List expressions = Arrays.asList(matchingExpression);
    this.filterExpression.addAll(expressions);
    this.hierarchy = hierarchy;
    this.type = MdxFunctionType.Filter;
  }

  public NameFilter(Hierarchy hierarchy, List<String> matchingExpression) {
    this.hierarchy = hierarchy;
    this.filterExpression.addAll(matchingExpression);
    this.type = MdxFunctionType.Filter;
  }

  public NameFilter(Hierarchy hierarchy, List<String> matchingExpression, String operator) {
    this.hierarchy = hierarchy;
    this.filterExpression.addAll(matchingExpression);
    this.type = MdxFunctionType.Filter;
    this.op = operator;
    if(operator != null && operator.equals("NOTEQUAL")) {
      this.operator = " <> ";
    }

  }

  public List<ParseTreeNode> getArguments(MdxParser parser) {
    ArrayList filters = new ArrayList();
    ArrayList arguments = new ArrayList();
    HierarchyNode h = new HierarchyNode((ParseRegion)null, this.hierarchy);

    for(int allfilter = 0; allfilter < this.filterExpression.size(); ++allfilter) {
      String i = (String)this.filterExpression.get(allfilter);
      String o = this.operator;
      if(this.filterExpression.size() > 1 && allfilter == 0) {
	o = " = ";
      }

      LiteralNode filterExp = LiteralNode.createString((ParseRegion)null, i);
      CallNode currentMemberNode = new CallNode((ParseRegion)null, "CurrentMember", Syntax.Property, new ParseTreeNode[]{h});
      CallNode currentMemberNameNode = new CallNode((ParseRegion)null, "Name", Syntax.Property, new ParseTreeNode[]{currentMemberNode});
      CallNode filterNode = new CallNode((ParseRegion)null, o, Syntax.Infix, new ParseTreeNode[]{currentMemberNameNode, filterExp});
      filters.add(filterNode);
    }

    if(filters.size() == 1) {
      arguments.addAll(filters);
    } else if(filters.size() > 1) {
      Object var12 = (ParseTreeNode)filters.get(0);

      for(int var13 = 1; var13 < filters.size(); ++var13) {
	var12 = new CallNode((ParseRegion)null, " OR ", Syntax.Infix, new ParseTreeNode[]{(ParseTreeNode)var12, (ParseTreeNode)filters.get(var13)});
      }

      arguments.add(var12);
    }

    return arguments;
  }

  public MdxFunctionType getFunctionType() {
    return this.type;
  }

  public List<String> getFilterExpression() {
    return this.filterExpression;
  }

  public Hierarchy getHierarchy() {
    return this.hierarchy;
  }

  public String getOp() {
    return this.op;
  }
}