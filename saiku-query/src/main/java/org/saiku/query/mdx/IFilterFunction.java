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

import java.util.List;

public interface IFilterFunction {
  IFilterFunction.MdxFunctionType getFunctionType();

  List<ParseTreeNode> getArguments(MdxParser var1);

  ParseTreeNode visit(MdxParser var1, ParseTreeNode var2);

  enum MdxFunctionType {
    Filter,
    TopCount,
    TopPercent,
    TopSum,
    BottomCount,
    BottomPercent,
    BottomSum;

    MdxFunctionType() {
    }
  }
}
