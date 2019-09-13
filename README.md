# Liberty Dev Mode Demo

### Quick Links
[Dev mode documentation](https://github.com/OpenLiberty/ci.maven/blob/master/docs/dev.md)  
[Liberty Dev VS Code extension](https://marketplace.visualstudio.com/items?itemName=Open-Liberty.liberty-dev-vscode-ext)

## Demo scenario
1. Clone this repo.

2. Run `mvn liberty:dev` to start dev mode.

3. Add `mpHealth-2.0` feature to `src/main/liberty/config/server.xml`. You can now access the http://localhost:9080/health endpoint (though it's just an empty array).

<details>
    <summary>4. Create the src/main/java/io/openliberty/sample/system/SystemLivenessCheck.java class.  Changes are reflected in the http://localhost:9080/health endpoint.  </summary>

```
package io.openliberty.sample.system;

import javax.enterprise.context.ApplicationScoped;

import java.lang.management.MemoryMXBean;
import java.lang.management.ManagementFactory;

import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

@Liveness
@ApplicationScoped
public class SystemLivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long memUsed = memBean.getHeapMemoryUsage().getUsed();
        long memMax = memBean.getHeapMemoryUsage().getMax();
  
        return HealthCheckResponse.named(
            SystemResource.class.getSimpleName() + " liveness check")
                                  .withData("memory used", memUsed)
                                  .withData("memory max", memMax)
                                  .state(memUsed < memMax * 0.9).build();
    }
    
}
```
</details>


5. Go to the console where you started dev mode, and press Enter.  The integration tests are run on a separate thread while dev mode is still active.

<details>
    <summary>6. Create the src/main/java/io/openliberty/sample/system/SystemReadinessCheck.java class.  Changes are reflected in the http://localhost:9080/health endpoint. </summary>

```
package io.openliberty.sample.system;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

@Readiness
@ApplicationScoped
public class SystemReadinessCheck implements HealthCheck {

    @Inject
    @ConfigProperty(name = "io_openliberty_guides_system_inMaintenance")
    Provider<String> inMaintenance;
	
    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(
		SystemResource.class.getSimpleName() + " readiness check");
        if (inMaintenance != null && inMaintenance.get().equalsIgnoreCase("true")) {
            return builder.withData("services", "not available").down().build();
        }
        return builder.withData("services", "available").up().build();
    }
    
}
```
</details>

7. Change the `io_openliberty_guides_system_inMaintenance` variable in `src/main/liberty/config/server.xml` to true.  Changes are reflected in the http://localhost:9080/health endpoint.  Undo this afterwards.


8. Make changes to the `src/main/webapp/index.html` (or any other webapp files). Changes are reflected on the home page http://localhost:9080/.

<details>
    <summary>9. Create the src/test/java/it/io/openliberty/sample/HealthEndpointIT.java class as an integration test. Press Enter in the console. The tests are run and should pass. </summary>
    
```
package it.io.openliberty.sample;

import static org.junit.Assert.assertEquals;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HealthEndpointIT {
    
    private static String baseUrl;
    private static final String HEALTH_ENDPOINT = "/health";
    private static final String LIVENESS_ENDPOINT = "/health/live";
    private static final String READINESS_ENDPOINT = "/health/ready";
    
    private Client client;
    private Response response;
    
    @BeforeClass
    public static void oneTimeSetup() {
        String port = System.getProperty("liberty.test.port", "9080");
        baseUrl = "http://localhost:" + port;
    }
    
    @Before
    public void setup() {
        response = null;
        client = ClientBuilder.newClient();
        client.register(JsrJsonpProvider.class);
    }
    
    @After
    public void teardown() {
        response.close();
        client.close();
    }

    @Test
    public void testHealthEndpoint() {
        String healthURL = baseUrl + HEALTH_ENDPOINT;
        response = this.getResponse(baseUrl + HEALTH_ENDPOINT);
        this.assertResponse(healthURL, response);
        
        JsonObject healthJson = response.readEntity(JsonObject.class);
        String expectedOutcome = "UP";
        String actualOutcome = healthJson.getString("status");
        assertEquals("Application should be healthy", expectedOutcome, actualOutcome);
       
        JsonObject healthCheck = healthJson.getJsonArray("checks").getJsonObject(0);
        String healthCheckName = healthCheck.getString("name");
        actualOutcome = healthCheck.getString("status");
        assertEquals(healthCheckName + " wasn't healthy", expectedOutcome, actualOutcome);

        healthCheck = healthJson.getJsonArray("checks").getJsonObject(1);
        healthCheckName = healthCheck.getString("name");
        actualOutcome = healthCheck.getString("status");
        assertEquals(healthCheckName + " wasn't healthy", expectedOutcome, actualOutcome);
    }

    @Test
    public void testLivenessEndpoint() {
        String livenessURL = baseUrl + LIVENESS_ENDPOINT;
        response = this.getResponse(baseUrl + LIVENESS_ENDPOINT);
        this.assertResponse(livenessURL, response);
        
        JsonObject healthJson = response.readEntity(JsonObject.class);
        String expectedOutcome = "UP";
        String actualOutcome = healthJson.getString("status");
        assertEquals("Applications liveness check passed", expectedOutcome, actualOutcome);
    }

    @Test
    public void testReadinessEndpoint() {
        String readinessURL = baseUrl + READINESS_ENDPOINT;
        response = this.getResponse(baseUrl + READINESS_ENDPOINT);
        this.assertResponse(readinessURL, response);
        
        JsonObject healthJson = response.readEntity(JsonObject.class);
        String expectedOutcome = "UP";
        String actualOutcome = healthJson.getString("status");
        assertEquals("Applications readiness check passed", expectedOutcome, actualOutcome);
    }
   
    private Response getResponse(String url) {
        return client.target(url).request().get();
    }

    private void assertResponse(String url, Response response) {
        assertEquals("Incorrect response code from " + url, 200, response.getStatus());
    }

}
```
</details>

10. Connect to the debug port 7777 with a debugger.

11. When you are done use ctrl-c to terminate dev mode and stop your server.

## VS Code extension

1. In VS Code, go to the Extensions view (View > Extensions)
2. Search for `Liberty Dev`
3. Click `Install`
4. Go to the Explorer view (View > Explorer)
5. In the side bar, `Liberty Dev Dashboard` shows your Liberty dev projects.
6. Right-click your project to start, stop, and interact with dev mode as an alternative to using the console.
<img src="libertydev_vscode.png" width="50%" height="50%" title="Liberty Dev VS Code extension">
