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

import org.olap4j.mdx.LiteralNode;
import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.parser.MdxParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NFilter extends AbstractFilterFunction {
  private String filterExpression;
  private int n;
  private MdxFunctionType type;

  public NFilter(MdxFunctionType type, int n, String filterExpression) {
    if(MdxFunctionType.Filter.equals(type)) {
      throw new IllegalArgumentException("Cannot use Filter() as TopN Filter");
    } else {
      this.filterExpression = filterExpression;
      this.n = n;
      this.type = type;
    }
  }

  public int getN() {
    return this.n;
  }

  public String getFilterExpression() {
    return this.filterExpression;
  }

  public List<ParseTreeNode> getArguments(MdxParser parser) {
    ArrayList arguments = new ArrayList();
    LiteralNode nfilter = LiteralNode.createNumeric((ParseRegion)null, new BigDecimal(this.n), false);
    arguments.add(nfilter);
    if(this.filterExpression != null) {
      ParseTreeNode topn = parser.parseExpression(this.filterExpression);
      arguments.add(topn);
    }

    return arguments;
  }

  public MdxFunctionType getFunctionType() {
    return this.type;
  }
}
