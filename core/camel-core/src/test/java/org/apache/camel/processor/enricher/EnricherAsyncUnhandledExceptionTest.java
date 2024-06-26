/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor.enricher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExchangeException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.async.MyAsyncComponent;
import org.apache.camel.spi.ShutdownStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EnricherAsyncUnhandledExceptionTest extends ContextTestSupport {

    @Test
    public void testInOutWithRequestBody() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:pickedUp");
        mock.expectedMessageCount(1);
        // this direct endpoint should receive an exception
        try {
            Future<Object> obj = template.asyncRequestBody("direct:in", "Hello World");
            // wait five seconds at most; else, let's assume something went
            // wrong
            obj.get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // if we receive an exception, the async routing engine is working
            // correctly
            // before the Enricher was fixed for cases where routing was async
            // and the AggregationStrategy
            // threw an exception, the call to requestBody would stall
            // indefinitely
            // unwrap the exception chain
            boolean b3 = e instanceof ExecutionException;
            assertTrue(b3);
            boolean b2 = e.getCause() instanceof CamelExecutionException;
            assertTrue(b2);
            boolean b1 = e.getCause().getCause() instanceof CamelExchangeException;
            assertTrue(b1);
            boolean b = e.getCause().getCause().getCause() instanceof RuntimeException;
            assertTrue(b);
            assertEquals("Bang! Unhandled exception", e.getCause().getCause().getCause().getMessage());
            mock.assertIsSatisfied();
            return;
        }
        fail("Expected an RuntimeException");
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        ShutdownStrategy shutdownStrategy = camelContext.getShutdownStrategy();
        camelContext.addComponent("async", new MyAsyncComponent());
        shutdownStrategy.setTimeout(1000);
        shutdownStrategy.setTimeUnit(TimeUnit.MILLISECONDS);
        shutdownStrategy.setShutdownNowOnTimeout(true);
        return camelContext;
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("direct:in").to("mock:pickedUp")
                        // using the async utility component to ensure that the
                        // async routing engine kicks in
                        .enrich("async:out?reply=Reply", new AggregationStrategy() {
                            @Override
                            public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                                throw new RuntimeException("Bang! Unhandled exception");
                            }
                        });

            }
        };
    }

}
