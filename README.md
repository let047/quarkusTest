# Quarkus getting-started project with unresolvable reflection

This is an artificial example showing how Magicator discovered codepaths that were not visible to tracing agent.

src/main/java/org/acme/getting/started/GreetingResource.java 
...has reflection calls in it that are activated if environment variable PROD=1:

```@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws ClassNotFoundException {
        boolean flag = false;
        if (null != System.getenv("PROD") && System.getenv("PROD").equals("1"))
            flag = true;
        if (flag) {
            for (int methodToCallCount=0; methodToCallCount <= 7; methodToCallCount++) {
                for (int classCount = 0; classCount <= 15; classCount++) {
                    Class thisClass = Class.forName("io.manycore.reflection.Meng" + classCount);
                    System.out.println(thisClass.toString());
                }
            }
            return "Flag was on. Did loads of reflection";
        } else {
            return "Flag is off";
        }
    }
}
```

Third party libraries are generally where you find reflection calls, as it makes the libraries flexible.
It does however cause problems with native-image, where everything needs to be known at compile time (and
is one of the reasons behind Quarkus).

We ran the fat jar through manycore, and the config generated is in src/main/resources.

To compile the project with a containerized native-image:
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

...or with your own:
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=false
```

### To test

```shell script
getting-started ehsmeng> unset PROD
getting-started ehsmeng> ./mvnw package -Pnative -Dquarkus.native.container-build=true
getting-started ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
io.manycore.Main Starting...
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT native (powered by Quarkus 1.13.2.Final) started in 0.006s. Listening on: http://0.0.0.0:8080
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) Profile prod activated. 
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy]
Do startup logic here
```
...and in another terminal:
```shell script
getting-started ehsmeng> curl 'http://localhost:8080/hello'
Flag is off
```
...go back to the first terminal and ^c the running server
```shell script
^C2021-04-20 18:57:45,969 INFO  [io.quarkus] (Shutdown thread) getting-started stopped in 0.003s
getting-started ehsmeng> export PROD=1
getting-started ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
io.manycore.Main Starting...
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT native (powered by Quarkus 1.13.2.Final) started in 0.006s. Listening on: http://0.0.0.0:8080
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) Profile prod activated. 
2021-04-20 18:55:12,727 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy]
Do startup logic here
```

...and in the other terminal:
```shell script
getting-started ehsmeng> curl 'http://localhost:8080/hello'
Flag was on. Did loads of reflection
```

### To test the crash
```shell script
getting-started ehsmeng> echo '[]' > src/main/resources/reflect-config.json
```
...and compile and redo the tests above. Now the server will crash if you start it with PROD=1 and request the /hello page.