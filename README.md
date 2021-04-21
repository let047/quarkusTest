# Quarkus getting-started project with unresolvable reflection

This is an artificial example showing how Magicator discovered codepaths that were not visible to tracing agent.

[GreetingResource.java](https://github.com/let047/quarkusTest/blob/main/src/main/java/org/acme/getting/started/GreetingResource.java) has reflection calls in it that are activated if environment variable PROD=1:

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

We ran the fat jar through Magicator, and the config generated is in src/main/resources. Magicator is a tool
to help with porting to native-image, and it figures our dynamic dependencis that tracing-agent misses. Magicator
analyzes your Jar. Tracing agent records what you're running. These two are complementary.

Let's try!

To download and compile the project with a containerized native-image:
```shell script
tmp ehsmeng> git clone https://github.com/let047/quarkusTest.git
tmp ehsmeng> cd quarkusTest
quarkusTest ehsmeng> unset PROD
quarkusTest ehsmeng> ./mvnw package -Pnative -Dquarkus.native.container-build=true
```

...or to compile with your own native-image (the project is Java 11 based):
```shell script
quarkusTest ehsmeng> ./mvnw package -Pnative -Dquarkus.native.container-build=false
```

Note! This project has the Magicator generated GraalVM native-image configuration files included.
First we will show that the example works with it, and then that it crashes without.

### To test

```shell script
quarkusTest ehsmeng> unset PROD
quarkusTest ehsmeng> ./mvnw package -Pnative -Dquarkus.native.container-build=true
quarkusTest ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
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
quarkusTest ehsmeng> curl 'http://localhost:8080/hello'
Flag is off
```
...go back to the first terminal and ^c the running server
```shell script
^C2021-04-20 18:57:45,969 INFO  [io.quarkus] (Shutdown thread) getting-started stopped in 0.003s
quarkusTest ehsmeng> export PROD=1
quarkusTest ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
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
quarkusTest ehsmeng> curl 'http://localhost:8080/hello'
Flag was on. Did loads of reflection
```

### To test the crash
Stop the running server if any.
```shell script
^C2021-04-20 19:07:45,939 INFO  [io.quarkus] (Shutdown thread) getting-started stopped in 0.003s
quarkusTest ehsmeng> echo '[]' > src/main/resources/reflect-config.json
quarkusTest ehsmeng> unset PROD
quarkusTest ehsmeng> ./mvnw package -Pnative -Dquarkus.native.container-build=true
quarkusTest ehsmeng> export PROD=1
quarkusTest ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
io.manycore.Main Starting...
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2021-04-21 13:50:49,503 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT native (powered by Quarkus 1.13.2.Final) started in 0.021s. Listening on: http://0.0.0.0:8080
2021-04-21 13:50:49,504 INFO  [io.quarkus] (main) Profile prod activated. 
2021-04-21 13:50:49,504 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy]
Do startup logic here
```

In the other terminal:
```shell script
quarkusTest ehsmeng> curl 'http://localhost:8080/hello'
<!doctype html>
<html lang="en">
<head>
    <title>Internal Server Error - Error id 43114609-9509-4794-be78-abd359981507-2</title>
    <meta charset="utf-8">
    <style>
html, body {
```

Which will upset the running server:
```shell script
quarkusTest ehsmeng> target/getting-started-1.0.0-SNAPSHOT-runner
io.manycore.Main Starting...
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2021-04-21 13:50:49,503 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT native (powered by Quarkus 1.13.2.Final) started in 0.021s. Listening on: http://0.0.0.0:8080
2021-04-21 13:50:49,504 INFO  [io.quarkus] (main) Profile prod activated. 
2021-04-21 13:50:49,504 INFO  [io.quarkus] (main) Installed features: [cdi, resteasy]
Do startup logic here
2021-04-21 13:50:51,739 ERROR [io.qua.ver.htt.run.QuarkusErrorHandler] (executor-thread-1) HTTP Request to /hello failed, error id: 43114609-9509-4794-be78-abd359981507-1: org.jboss.resteasy.spi.UnhandledException: java.lang.ClassNotFoundException: io.manycore.reflection.Meng0
	at org.jboss.resteasy.core.ExceptionHandler.handleApplicationException(ExceptionHandler.java:106)
	at org.jboss.resteasy.core.ExceptionHandler.handleException(ExceptionHandler.java:372)
	at org.jboss.resteasy.core.SynchronousDispatcher.writeException(SynchronousDispatcher.java:218)
	at org.jboss.resteasy.core.SynchronousDispatcher.invoke(SynchronousDispatcher.java:519)
	at org.jboss.resteasy.core.SynchronousDispatcher.lambda$invoke$4(SynchronousDispatcher.java:261)
	at org.jboss.resteasy.core.SynchronousDispatcher.lambda$preprocess$0(SynchronousDispatcher.java:161)
	at org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext.filter(PreMatchContainerRequestContext.java:364)
	at org.jboss.resteasy.core.SynchronousDispatcher.preprocess(SynchronousDispatcher.java:164)
```

Magicator does not cover everything. It's a compliment to native-agent.

[You're welcome to use our free tool](https://staticizer.magicator.com/): 
Upload your fat jar with Main-Class specified in META-INF/MANIFEST.MF. We'll create native-image configuration files for you.<br>
[Here is the auto-generated reflection file for this project](https://github.com/let047/quarkusTest/blob/main/src/main/resources/reflect-config.json)