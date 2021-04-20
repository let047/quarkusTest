package org.acme.getting.started;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
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