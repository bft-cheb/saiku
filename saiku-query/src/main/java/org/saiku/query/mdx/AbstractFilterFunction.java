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
import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.Syntax;
import org.olap4j.mdx.parser.MdxParser;

import java.util.List;

public abstract class AbstractFilterFunction implements IFilterFunction {
  public AbstractFilterFunction() {
  }

  public ParseTreeNode visit(MdxParser parser, ParseTreeNode parent) {
    List arguments = this.getArguments(parser);
    arguments.add(0, parent);
    return new CallNode((ParseRegion) null, this.getFunctionType().toString(), Syntax.Function, arguments);
  }
}