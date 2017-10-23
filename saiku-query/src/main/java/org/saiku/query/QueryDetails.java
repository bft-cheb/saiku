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

import org.olap4j.Axis;
import org.olap4j.impl.Named;
import org.olap4j.metadata.Measure;

import java.util.ArrayList;
import java.util.List;

public class QueryDetails implements Named {
  protected List<Measure> measures = new ArrayList();
  private QueryDetails.Location location;
  private Axis axis;
  private Query query;

  public QueryDetails(Query query, Axis axis) {
    this.location = QueryDetails.Location.BOTTOM;
    this.axis = axis;
    this.query = query;
  }

  public void add(Measure measure) {
    if(!this.measures.contains(measure)) {
      this.measures.add(measure);
    }

  }

  public void set(Measure measure, int position) {
    if(!this.measures.contains(measure)) {
      this.measures.add(position, measure);
    } else {
      int oldindex = this.measures.indexOf(measure);
      if(oldindex <= position) {
	this.measures.add(position, measure);
	this.measures.remove(oldindex);
      }
    }

  }

  public void remove(Measure measure) {
    this.measures.remove(measure);
  }

  public List<Measure> getMeasures() {
    return this.measures;
  }

  public QueryDetails.Location getLocation() {
    return this.location;
  }

  public void setLocation(QueryDetails.Location location) {
    this.location = location;
  }

  public Axis getAxis() {
    return this.axis;
  }

  public void setAxis(Axis axis) {
    this.axis = axis;
  }

  public String getName() {
    return "DETAILS";
  }

  public static enum Location {
    TOP,
    BOTTOM;

    private Location() {
    }
  }
}
