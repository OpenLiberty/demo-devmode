# Liberty dev mode demo with Gradle

Converting the Liberty Maven Plugin dev mode demo to Gradle.  There is not currently Liberty dev mode support for Gradle.

* Assemble the application (build without running tests): `gradle assemble`
* Start Liberty server: `gradle libertyStart`
* Run integration and unit tests: `gradle check`
* Stop Liberty server: `gradle libertyStop`

## Demo scenario

### Hot deployment

1. Clone this repo

2. Run `gradle libertyDev` to start dev mode.

3. Add `mpHealth-2.0` feature to `src/main/liberty/config/server.xml`. You can now access the http://localhost:9080/health endpoint (though it's just an empty array).

<details>
    <summary>4. Create the src/main/java/io/openliberty/sample/system/SystemLivenessCheck.java class.  Changes are reflected in the http://localhost:9080/health endpoint.  </summary>

```java
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

<details>
    <summary>5. Create the src/main/java/io/openliberty/sample/system/SystemReadinessCheck.java class.  Changes are reflected in the http://localhost:9080/health endpoint. </summary>

```java
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

6. Change the `io_openliberty_guides_system_inMaintenance` variable in `src/main/liberty/config/server.xml` to `true`.  Changes are reflected in the http://localhost:9080/health endpoint.  Undo this afterwards.

7. Make changes to the `src/main/webapp/index.html` (or any other webapp files). Changes are reflected on the home page http://localhost:9080/.

### Hot testing

1. Go to the console where you started dev mode, and press Enter.  The integration tests are run on a separate thread while dev mode is still active.

<details>
    <summary>2. Create the src/test/java/it/io/openliberty/sample/HealthEndpointIT.java class as an integration test. Notice the "liberty.hostname" and "liberty.http.port" system properties which are provided by dev mode when running integration tests.  Press Enter in the console to run the tests. They should pass. </summary>
    
```java
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
        String hostname = System.getProperty("liberty.hostname", "localhost");
        String port = System.getProperty("liberty.http.port", "9080");
        baseUrl = "http://" + hostname + ":" + port + "/";
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

3. Stop dev mode by pressing Ctrl-C in the console.

4. Run `gradle libertyDev --hotTests` to enable hot testing.

5. Notice tests are run immediately after dev mode starts up.

6. In `src/main/java/io/openliberty/sample/system/SystemResource.java`, change the annotation `@Path("/properties")` to `@Path("/properties2")`.

7. Notice tests are run automatically after the source change, and a test fails because the endpoint path is wrong.

8. Revert the annotation back to `@Path("/properties")`.

9. Notice tests are run automatically and pass.

### Hot debugging

1. Run `gradle libertyDev --libertyDebug` to start dev mode with debugging.

2. In `src/main/java/io/openliberty/sample/system/SystemLivenessCheck.java`, set a breakpoint inside the `call()` method.

3. Attach your IDE's debugger to port `7777`.  
For example, in VS Code, click `Debug` > `Add Configuration...` > `Java: Attach` > set `"port": "7777"`.  Then `View` > `Debug` > select `Debug (Attach)`, then press the arrow icon to start debugging.

4. In your browser, go to http://localhost:9080/health.

5. Notice your IDE pauses at the breakpoint that you set, allowing you to debug.

6. Disconnect the debugger.

7. When you are done, press Ctrl-C in the console to terminate dev mode and stop your server.
