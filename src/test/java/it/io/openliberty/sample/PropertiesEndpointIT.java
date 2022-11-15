// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.sample;

import static org.junit.Assert.assertEquals;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.junit.Test;

public class PropertiesEndpointIT {

  @Test
  public void testGetProperties() {

      // system properties
      String hostname = System.getProperty("liberty.hostname", "localhost");
      String port = System.getProperty("liberty.http.port", "9080");
      String url = "http://" + hostname + ":" + port + "/";

      // client setup
      Client client = ClientBuilder.newClient();
      client.register(JsrJsonpProvider.class);

      // request
      WebTarget target = client.target(url + "system/properties");
      Response response = target.request().get();

      // response
      assertEquals("Incorrect response code from " + url, 200, response.getStatus());

      JsonObject obj = response.readEntity(JsonObject.class);

      assertEquals("The system property for the server output directory should match with the Open Liberty container image.",
                   "/opt/ol/wlp/output/defaultServer/",
                   obj.getString("server.output.dir"));

      response.close();
  }
  
}
