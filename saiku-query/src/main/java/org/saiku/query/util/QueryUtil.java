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
package org.saiku.query.util;

import org.apache.commons.lang.StringUtils;
import org.saiku.query.IQuerySet;
import org.saiku.query.ISortableQuerySet;

import java.util.ArrayList;
import java.util.List;

public class QueryUtil {
  public QueryUtil() {
  }

  public static List<String> parseParameters(String expression) {
    ArrayList parameterNames = new ArrayList();
    if(StringUtils.isNotBlank(expression)) {
      char[] exArray = expression.toCharArray();

      for(int i = 0; i < exArray.length - 2; ++i) {
	if(exArray[i] == 36 && exArray[i + 1] == 123) {
	  StringBuffer sb = new StringBuffer();

	  for(i += 2; i < exArray.length && exArray[i] != 125; ++i) {
	    sb.append(exArray[i]);
	  }

	  String newParam = sb.toString();
	  if(StringUtils.isNotBlank(newParam)) {
	    parameterNames.add(newParam);
	  }
	}
      }
    }

    return parameterNames;
  }

  public static List<String> retrieveSortableSetParameters(ISortableQuerySet sqs) {
    ArrayList parameterNames = new ArrayList();
    String sortEval = sqs.getSortEvaluationLiteral();
    List sortParameters = parseParameters(sortEval);
    parameterNames.addAll(sortParameters);
    parameterNames.addAll(retrieveSetParameters(sqs));
    return parameterNames;
  }

  public static List<String> retrieveSetParameters(IQuerySet qs) {
    ArrayList parameterNames = new ArrayList();
    String mdx = qs.getMdxSetExpression();
    List mdxParameters = parseParameters(mdx);
    parameterNames.addAll(mdxParameters);
    return parameterNames;
  }
}
