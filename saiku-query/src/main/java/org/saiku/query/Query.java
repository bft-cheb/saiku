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
import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapException;
import org.olap4j.OlapStatement;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Catalog;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Member.Type;
import org.olap4j.metadata.NamedList;
import org.olap4j.metadata.Property;
import org.saiku.query.ISortableQuerySet.HierarchizeMode;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.query.metadata.CalculatedMember;
import org.saiku.query.util.QueryUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Query {
    protected final String name;
    protected Map<Axis, QueryAxis> axes = new HashMap();
    protected QueryAxis across;
    protected QueryAxis down;
    protected QueryAxis filter;
    protected QueryAxis unused;
    protected final Cube cube;
    protected Map<String, QueryHierarchy> hierarchyMap = new HashMap();
    protected NamedList<CalculatedMeasure> calculatedMeasures = new NamedListImpl();
    protected QueryDetails details;
    protected boolean selectDefaultMembers = true;
    private final OlapConnection connection;
    private HierarchizeMode defaultHierarchizeMode;
    private boolean visualTotals;
    private String visualTotalsPattern;
    private boolean lowestLevelsOnly;
    private Map<String, String> parameters;
    private Map<String, List<String>> aggregators;

    public Query(String name, Cube cube) throws SQLException {
        this.defaultHierarchizeMode = HierarchizeMode.PRE;
        this.visualTotals = false;
        this.lowestLevelsOnly = false;
        this.parameters = new HashMap();
        this.aggregators = new HashMap();
        this.name = name;
        this.cube = cube;
        Catalog catalog = cube.getSchema().getCatalog();
        this.connection = (OlapConnection)catalog.getMetaData().getConnection().unwrap(OlapConnection.class);
        this.connection.setCatalog(catalog.getName());
        this.unused = new QueryAxis(this, (Axis)null);
        Iterator i$ = cube.getHierarchies().iterator();

        while(i$.hasNext()) {
            Hierarchy hierarchy = (Hierarchy)i$.next();
            QueryHierarchy queryHierarchy = new QueryHierarchy(this, hierarchy);
            this.unused.getQueryHierarchies().add(queryHierarchy);
            this.hierarchyMap.put(queryHierarchy.getUniqueName(), queryHierarchy);
        }

        this.across = new QueryAxis(this, Axis.COLUMNS);
        this.down = new QueryAxis(this, Axis.ROWS);
        this.filter = new QueryAxis(this, Axis.FILTER);
        this.axes.put(null, this.unused);
        this.axes.put(Axis.COLUMNS, this.across);
        this.axes.put(Axis.ROWS, this.down);
        this.axes.put(Axis.FILTER, this.filter);
        this.details = new QueryDetails(this, Axis.COLUMNS);
    }

    public SelectNode getSelect() throws OlapException {
        try {
            return Olap4jNodeConverter.toQuery(this);
        } catch (Exception var2) {
            throw new OlapException("Error creating Select", var2);
        }
    }

    public String getMdx() throws OlapException {
        StringWriter writer = new StringWriter();
        this.getSelect().unparse(new ParseTreeWriter(new PrintWriter(writer)));
        return writer.toString();
    }

    public Cube getCube() {
        return this.cube;
    }

    public OlapConnection getConnection() {
        return this.connection;
    }

    public Catalog getCatalog() {
        return this.cube.getSchema().getCatalog();
    }

    public QueryHierarchy getHierarchy(String name) {
        if(this.hierarchyMap.containsKey(name)) {
            return (QueryHierarchy)this.hierarchyMap.get(name);
        } else {
            Iterator i$ = this.hierarchyMap.values().iterator();

            QueryHierarchy qh;
            do {
                if(!i$.hasNext()) {
                    return null;
                }

                qh = (QueryHierarchy)i$.next();
            } while(!qh.getName().equals(name));

            return qh;
        }
    }

    public QueryHierarchy getHierarchy(Hierarchy hierarchy) {
        return hierarchy == null?null:(QueryHierarchy)this.hierarchyMap.get(hierarchy.getUniqueName());
    }

    public QueryLevel getLevel(Hierarchy hierarchy, String name) {
        QueryHierarchy h = (QueryHierarchy)this.hierarchyMap.get(hierarchy.getUniqueName());
        return h.getActiveLevel(name);
    }

    public QueryLevel getLevel(Level level) {
        return this.getLevel(level.getHierarchy(), level.getName());
    }

    public QueryLevel getLevel(String uniqueLevelName) {
        if(StringUtils.isNotBlank(uniqueLevelName)) {
            Iterator i$ = this.hierarchyMap.values().iterator();

            while(i$.hasNext()) {
                QueryHierarchy qh = (QueryHierarchy)i$.next();
                Iterator i$1 = qh.getActiveQueryLevels().iterator();

                while(i$1.hasNext()) {
                    QueryLevel ql = (QueryLevel)i$1.next();
                    if(ql.getUniqueName().equals(uniqueLevelName)) {
                        return ql;
                    }
                }
            }
        }

        return null;
    }

    public void swapAxes() {
        if(this.axes.size() != 4) {
            throw new IllegalArgumentException();
        } else {
            ArrayList tmpAcross = new ArrayList();
            tmpAcross.addAll(this.across.getQueryHierarchies());
            ArrayList tmpDown = new ArrayList();
            tmpDown.addAll(this.down.getQueryHierarchies());
            this.across.getQueryHierarchies().clear();
            HashMap acrossChildList = new HashMap();

            for(int downChildList = 0; downChildList < tmpAcross.size(); ++downChildList) {
                acrossChildList.put(Integer.valueOf(downChildList), tmpAcross.get(downChildList));
            }

            this.down.getQueryHierarchies().clear();
            HashMap var6 = new HashMap();

            for(int cpt = 0; cpt < tmpDown.size(); ++cpt) {
                var6.put(Integer.valueOf(cpt), tmpDown.get(cpt));
            }

            this.across.getQueryHierarchies().addAll(tmpDown);
            this.down.getQueryHierarchies().addAll(tmpAcross);
        }
    }

    public QueryAxis getAxis(Axis axis) {
        return (QueryAxis)this.axes.get(axis);
    }

    public Map<Axis, QueryAxis> getAxes() {
        return this.axes;
    }

    public CalculatedMember createCalculatedMember(QueryHierarchy hierarchy, String name, String formula, Map<Property, Object> properties, boolean mondrian3) {
        Hierarchy h = hierarchy.getHierarchy();
        CalculatedMember cm = new CalculatedMember(h.getDimension(), h, name, name, (Member)null, Type.FORMULA, formula, (Map)null, mondrian3);
        this.addCalculatedMember(hierarchy, cm);
        return cm;
    }

    public CalculatedMember createCalculatedMember(QueryHierarchy hierarchy, Member parentMember, String name, String formula, Map<Property, Object> properties, boolean mondrian3) {
        Hierarchy h = hierarchy.getHierarchy();
        CalculatedMember cm = new CalculatedMember(h.getDimension(), h, name, name, parentMember, Type.FORMULA, formula, (Map)null, mondrian3);
        this.addCalculatedMember(hierarchy, cm);
        return cm;
    }

    public void addCalculatedMember(QueryHierarchy hierarchy, CalculatedMember cm) {
        hierarchy.addCalculatedMember(cm);
    }

    public NamedList<CalculatedMember> getCalculatedMembers(QueryHierarchy hierarchy) {
        return hierarchy.getCalculatedMembers();
    }

    public NamedList<CalculatedMember> getCalculatedMembers() {
        NamedListImpl cm = new NamedListImpl();
        Iterator i$ = this.hierarchyMap.values().iterator();

        while(i$.hasNext()) {
            QueryHierarchy h = (QueryHierarchy)i$.next();
            cm.addAll(h.getCalculatedMembers());
        }

        return cm;
    }

    public CalculatedMeasure createCalculatedMeasure(String name, String formula, Map<Property, Object> properties) {
        if(this.cube.getMeasures().size() > 0) {
            Measure first = (Measure)this.cube.getMeasures().get(0);
            return this.createCalculatedMeasure(first.getHierarchy(), name, formula, properties);
        } else {
            throw new RuntimeException("There has to be at least one valid measure in the cube to create a calculated measure!");
        }
    }

    public CalculatedMeasure createCalculatedMeasure(Hierarchy measureHierarchy, String name, String formula, Map<Property, Object> properties) {
        CalculatedMeasure cm = new CalculatedMeasure(measureHierarchy, name, name, formula, (Map)null);
        this.addCalculatedMeasure(cm);
        return cm;
    }

    public void addCalculatedMeasure(CalculatedMeasure cm) {
        this.calculatedMeasures.add(cm);
    }

    public NamedList<CalculatedMeasure> getCalculatedMeasures() {
        return this.calculatedMeasures;
    }

    public CalculatedMeasure getCalculatedMeasure(String name) {
        return (CalculatedMeasure)this.calculatedMeasures.get(name);
    }

    public Measure getMeasure(String name) {
        Iterator i$ = this.cube.getMeasures().iterator();

        Measure m;
        do {
            if(!i$.hasNext()) {
                return null;
            }

            m = (Measure)i$.next();
            if(name != null && name.equals(m.getName())) {
                return m;
            }
        } while(name == null || !m.getUniqueName().equals(name));

        return m;
    }

    public QueryDetails getDetails() {
        return this.details;
    }

    public QueryAxis getUnusedAxis() {
        return this.unused;
    }

    public CellSet execute() throws OlapException {
        SelectNode mdx = this.getSelect();
        Catalog catalog = this.getCatalog();

        try {
            this.connection.setCatalog(catalog.getName());
        } catch (SQLException var4) {
            throw new OlapException("Error while executing query", var4);
        }

        OlapStatement olapStatement = this.connection.createStatement();
        return olapStatement.executeOlapQuery(mdx);
    }

    public String getName() {
        return this.name;
    }

    public void moveHierarchy(QueryHierarchy hierarchy, Axis axis) {
        this.moveHierarchy(hierarchy, axis, -1);
    }

    public void moveHierarchy(QueryHierarchy hierarchy, Axis axis, int position) {
        QueryAxis oldQueryAxis = this.findAxis(hierarchy);
        QueryAxis newQueryAxis = this.getAxis(axis);
        if(oldQueryAxis != null && newQueryAxis != null && (position > -1 || oldQueryAxis.getLocation() != newQueryAxis.getLocation())) {
            if(oldQueryAxis.getLocation() != null) {
                oldQueryAxis.removeHierarchy(hierarchy);
            }

            if(newQueryAxis.getLocation() != null) {
                if(position > -1) {
                    newQueryAxis.addHierarchy(position, hierarchy);
                } else {
                    newQueryAxis.addHierarchy(hierarchy);
                }
            }
        }

    }

    private QueryAxis findAxis(QueryHierarchy hierarchy) {
        if(this.getUnusedAxis().getQueryHierarchies().contains(hierarchy)) {
            return this.getUnusedAxis();
        } else {
            Map axes = this.getAxes();
            Iterator i$ = axes.keySet().iterator();

            Axis axis;
            do {
                if(!i$.hasNext()) {
                    return null;
                }

                axis = (Axis)i$.next();
            } while(!((QueryAxis)axes.get(axis)).getQueryHierarchies().contains(hierarchy));

            return (QueryAxis)axes.get(axis);
        }
    }

    public void setSelectDefaultMembers(boolean selectDefaultMembers) {
        this.selectDefaultMembers = selectDefaultMembers;
    }

    public void setDefaultHierarchizeMode(HierarchizeMode mode) {
        this.defaultHierarchizeMode = mode;
    }

    public HierarchizeMode getDefaultHierarchizeMode() {
        return this.defaultHierarchizeMode;
    }

    public void setVisualTotals(boolean visualTotals) {
        if(!visualTotals) {
            this.visualTotalsPattern = null;
        }

        this.visualTotals = visualTotals;
    }

    public boolean isVisualTotals() {
        return this.visualTotals;
    }

    public void setVisualTotalsPattern(String pattern) {
        this.visualTotalsPattern = pattern;
    }

    public String getVisualTotalsPattern() {
        return this.visualTotalsPattern;
    }

    public void setLowestLevelsOnly(boolean lowest) {
        this.lowestLevelsOnly = lowest;
    }

    public boolean isLowestLevelsOnly() {
        return this.lowestLevelsOnly;
    }

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    public void retrieveParameters() {
        Iterator i$ = this.getAxes().values().iterator();

        while(i$.hasNext()) {
            QueryAxis qa = (QueryAxis)i$.next();
            Iterator qparams = qa.getQueryHierarchies().iterator();

            while(qparams.hasNext()) {
                QueryHierarchy qh = (QueryHierarchy)qparams.next();
                Iterator hparams = qh.getActiveQueryLevels().iterator();

                while(hparams.hasNext()) {
                    QueryLevel ql = (QueryLevel)hparams.next();
                    String pName = ql.getParameterName();
                    if(StringUtils.isNotBlank(pName)) {
                        this.addOrSetParameter(pName);
                    }

                    List params = QueryUtil.retrieveSetParameters(ql);
                    this.addOrSetParameters(params);
                }

                List hparams1 = QueryUtil.retrieveSortableSetParameters(qh);
                this.addOrSetParameters(hparams1);
            }

            List qparams1 = QueryUtil.retrieveSortableSetParameters(qa);
            this.addOrSetParameters(qparams1);
        }

    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String name, String value) {
        this.parameters.put(name, value);
    }

    public String getParameter(String parameter) {
        return this.parameters.containsKey(parameter)?(String)this.parameters.get(parameter):null;
    }

    public void addOrSetParameters(List<String> parameters) {
        if(parameters != null) {
            Iterator i$ = parameters.iterator();

            while(i$.hasNext()) {
                String param = (String)i$.next();
                this.addOrSetParameter(param);
            }
        }

    }

    public void addOrSetParameter(String parameter) {
        if(StringUtils.isNotBlank(parameter) && !this.parameters.containsKey(parameter)) {
            this.parameters.put(parameter, null);
        }

    }

    public List<String> getAggregators(String key) {
        return (List)(this.aggregators.containsKey(key)?(List)this.aggregators.get(key):new ArrayList());
    }

    public void setAggregators(String key, List<String> aggs) {
        if(StringUtils.isNotBlank(key) && aggs != null) {
            this.aggregators.put(key, aggs);
        }

    }

    public Query.BackendFlavor getFlavor() throws OlapException {
        String dataSourceInfo = this.connection.getOlapDatabase().getDataSourceInfo();
        String proivder = this.connection.getOlapDatabase().getProviderName();
        Query.BackendFlavor[] arr$ = Query.BackendFlavor.values();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Query.BackendFlavor flavor = arr$[i$];
            if(proivder.contains(flavor.token) || dataSourceInfo.contains(flavor.token)) {
                return flavor;
            }
        }

        throw new AssertionError("Can\'t determine the backend vendor. (" + dataSourceInfo + ")");
    }

    public enum BackendFlavor {
        MONDRIAN("Mondrian"),
        SSAS("Microsoft"),
        PALO("Palo"),
        SAP("SAP"),
        ESSBASE("Essbase"),
        UNKNOWN("");

        private final String token;

        BackendFlavor(String token) {
            this.token = token;
        }
    }
}
