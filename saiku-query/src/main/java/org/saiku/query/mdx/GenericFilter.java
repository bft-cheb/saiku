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

import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.parser.MdxParser;

import java.util.ArrayList;
import java.util.List;

public class GenericFilter extends AbstractFilterFunction {
  private String filterExpression;
  private MdxFunctionType type;

  public GenericFilter(String filterExpression) {
    this.filterExpression = filterExpression;
    this.type = MdxFunctionType.Filter;
  }

  public String getFilterExpression() {
    return this.filterExpression;
  }

  public List<ParseTreeNode> getArguments(MdxParser parser) {
    ArrayList arguments = new ArrayList();
    ParseTreeNode filterExp = parser.parseExpression(this.filterExpression);
    arguments.add(filterExp);
    return arguments;
  }

  public MdxFunctionType getFunctionType() {
    return this.type;
  }
}