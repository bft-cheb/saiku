/*  
 *   Copyright 2012 OSBI Ltd
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
package org.saiku.web.rest.resources;

import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.util.exception.SaikuServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

@Component
@Path("/saiku/{username}/datasources")
public class DataSourceResource {
  private String olapServerSchemaPath;

  DatasourceService datasourceService;

  private static final Logger log = LoggerFactory.getLogger(DataSourceResource.class);

  @Value("${olapserver.schemas.path}")
  private void setOlapServerSchemaPath(String value) {
    this.olapServerSchemaPath = value;
  }

  public void setDatasourceService(DatasourceService ds) {
    datasourceService = ds;
  }

  /**
   * Get Data Sources.
   *
   * @return A Collection of SaikuDatasource's.
   */
  @GET
  @Produces({"application/json"})
  public Collection<SaikuDatasource> getDatasources() {
    try {
      return datasourceService.getDatasources().values();
    } catch (SaikuServiceException e) {
      log.error(this.getClass().getName(), e);
      return new ArrayList<SaikuDatasource>();
    }
  }

  /**
   * Delete Data Source.
   *
   * @param datasourceName - The name of the data source.
   * @return A GONE Status.
   */
  @DELETE
  @Produces({"application/json"})
  @Path("/{datasource}")
  public Response deleteDatasource(@PathParam("datasource") String datasourceName) {
    datasourceService.removeDatasource(datasourceName);
    return Response.ok().build();
  }

  /**
   * Get Data Source.
   *
   * @param datasourceName.
   * @return A Saiku Datasource.
   */
  @GET
  @Produces({"application/json"})
  @Path("/{datasource}")
  public SaikuDatasource getDatasource(@PathParam("datasource") String datasourceName) {
    return datasourceService.getDatasource(datasourceName);
  }

  @POST
  @Path("/add")
  public Response addDatasource(@FormParam("datasource") String name, @FormParam("properties") String properties) {
    properties = properties.replace("res:schemas", olapServerSchemaPath);
    Properties datasourceProps = new Properties();
    for (String prop : properties.split("\n")) {
      datasourceProps.setProperty(prop.substring(0, prop.indexOf("=")), prop.substring(prop.indexOf("=") + 1));
    }
    SaikuDatasource ds = new SaikuDatasource(name, SaikuDatasource.Type.OLAP, datasourceProps);
    System.out.println("ds not null:" + (ds != null));
    System.out.println("ds name:" + ds.getName());
    datasourceService.addDatasource(ds);
    return Response.ok().build();
  }

}
