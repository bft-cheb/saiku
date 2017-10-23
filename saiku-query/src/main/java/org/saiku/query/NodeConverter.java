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

import org.olap4j.mdx.CallNode;
import org.olap4j.mdx.IdentifierNode;
import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.MemberNode;
import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.PropertyValueNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.WithMemberNode;
import org.olap4j.mdx.parser.impl.DefaultMdxParserImpl;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.saiku.query.metadata.Calculated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class NodeConverter {
  public NodeConverter() {
  }

  protected static CallNode generateSetCall(ParseTreeNode... args) {
    return new CallNode((ParseRegion)null, "{}", Syntax.Braces, args);
  }

  protected static CallNode generateListSetCall(List<ParseTreeNode> cnodes) {
    return new CallNode((ParseRegion)null, "{}", Syntax.Braces, cnodes);
  }

  protected static CallNode generateListTupleCall(List<ParseTreeNode> cnodes) {
    return new CallNode((ParseRegion)null, "()", Syntax.Parentheses, cnodes);
  }

  protected static CallNode generateCrossJoin(List<ParseTreeNode> selections) {
    return generateCrossJoin(selections, false, true);
  }

  protected static CallNode generateCrossJoin(List<ParseTreeNode> selections, boolean nonEmpty, boolean asterisk) {
    String crossJoinFun = nonEmpty?"NonEmptyCrossJoin":"CrossJoin";
    if(selections != null && selections.size() != 0) {
      Object sel1 = (ParseTreeNode)selections.get(0);
      if(sel1 instanceof MemberNode) {
	sel1 = generateSetCall(new ParseTreeNode[]{(ParseTreeNode)sel1});
      }

      if(selections.size() == 2) {
	Object sel2 = (ParseTreeNode)selections.get(1);
	if(sel2 instanceof MemberNode) {
	  sel2 = generateSetCall(new ParseTreeNode[]{(ParseTreeNode)sel2});
	}

	return new CallNode((ParseRegion)null, crossJoinFun, Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)sel1, (ParseTreeNode)sel2});
      } else if(asterisk) {
	return new CallNode((ParseRegion)null, " * ", Syntax.Infix, selections);
      } else {
	selections.remove(0);
	return new CallNode((ParseRegion)null, crossJoinFun, Syntax.Function, new ParseTreeNode[]{(ParseTreeNode)sel1, generateCrossJoin(selections, nonEmpty, asterisk)});
      }
    } else {
      return null;
    }
  }

  protected static CallNode generateUnion(List<List<ParseTreeNode>> unions) {
    if(unions.size() > 2) {
      List first = (List)unions.remove(0);
      return new CallNode((ParseRegion)null, "Union", Syntax.Function, new ParseTreeNode[]{generateCrossJoin(first), generateUnion(unions)});
    } else {
      return new CallNode((ParseRegion)null, "Union", Syntax.Function, new ParseTreeNode[]{generateCrossJoin((List)unions.get(0)), generateCrossJoin((List)unions.get(1))});
    }
  }

  protected static CallNode generateHierarchizeUnion(List<List<ParseTreeNode>> unions) {
    return new CallNode((ParseRegion)null, "Hierarchize", Syntax.Function, new ParseTreeNode[]{generateUnion(unions)});
  }

  protected static ParseTreeNode toOlap4jMemberSet(List<Member> members) {
    ArrayList membernodes = new ArrayList();
    Iterator i$ = members.iterator();

    while(i$.hasNext()) {
      Member m = (Member)i$.next();
      membernodes.add(new MemberNode((ParseRegion)null, m));
    }

    return generateListSetCall(membernodes);
  }

  protected static ParseTreeNode toOlap4jMeasureSet(List<Measure> measures) {
    ArrayList membernodes = new ArrayList();
    Iterator i$ = measures.iterator();

    while(i$.hasNext()) {
      Measure m = (Measure)i$.next();
      membernodes.add(new MemberNode((ParseRegion)null, m));
    }

    return generateListSetCall(membernodes);
  }

  protected static WithMemberNode toOlap4jCalculatedMember(Calculated cm) {
    DefaultMdxParserImpl parser = new DefaultMdxParserImpl();
    ParseTreeNode formula = parser.parseExpression(cm.getFormula());
    ArrayList propertyList = new ArrayList();
    Iterator wm = cm.getFormatProperties().entrySet().iterator();

    while(wm.hasNext()) {
      Entry entry = (Entry)wm.next();
      LiteralNode exp = LiteralNode.createString((ParseRegion)null, (String)entry.getValue());
      String name = (String)entry.getKey();
      PropertyValueNode prop = new PropertyValueNode((ParseRegion)null, name, exp);
      propertyList.add(prop);
    }

    WithMemberNode wm1 = new WithMemberNode((ParseRegion)null, IdentifierNode.parseIdentifier(cm.getUniqueName()), formula, propertyList);
    return wm1;
  }

  protected static IdentifierNode getIdentifier(String... identifiers) {
    String identifier = "";

    for(int i = 0; i < identifiers.length; ++i) {
      if(i == 0) {
	identifier = "~" + identifiers[0];
      } else {
	identifier = identifier + "_" + identifiers[i];
      }
    }

    return IdentifierNode.ofNames(new String[]{identifier});
  }
}
