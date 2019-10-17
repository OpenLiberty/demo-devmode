# Liberty dev mode demo with Gradle

Converting the Liberty Maven Plugin dev mode demo to Gradle.  There is not currently Liberty dev mode support for Gradle.

* Assemble the application (build without running tests): `gradle assemble`
* Start Liberty server: `gradle libertyStart`
* Run integration and unit tests: `gradle check` 
* Stop Liberty server: `gradle libertyStop`