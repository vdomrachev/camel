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
package org.apache.camel.component.djl;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CvInstanceSegmentationTest extends CamelTestSupport {

    @BeforeAll
    public static void setupDefaultEngine() {
        // Since Apache MXNet is discontinued, prefer PyTorch as the default engine
        System.setProperty("ai.djl.default_engine", "PyTorch");
    }

    @Test
    void testDJL() throws Exception {
        var mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(3);
        mock.await();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("file:src/test/resources/data/detect?recursive=true&noop=true")
                        .convertBodyTo(byte[].class)
                        .to("djl:cv/instance_segmentation?artifactId=ai.djl.mxnet:mask_rcnn:0.0.1")
                        .log("${header.CamelFileName} = ${body}")
                        .to("mock:result");
            }
        };
    }

}
