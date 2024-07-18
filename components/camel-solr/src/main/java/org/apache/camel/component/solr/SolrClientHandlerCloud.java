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
package org.apache.camel.component.solr;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

public class SolrClientHandlerCloud extends SolrClientHandler {

    public SolrClientHandlerCloud(SolrConfiguration solrConfiguration) {
        super(solrConfiguration);
    }

    protected SolrClient getSolrClient() {
        Optional<String> zkChrootOptional = Optional.ofNullable(solrConfiguration.getZkChroot());
        List<String> urlList = getUrlListFrom(solrConfiguration);
        CloudHttp2SolrClient.Builder builder = new CloudHttp2SolrClient.Builder(
                urlList,
                zkChrootOptional);
        if (solrConfiguration.getCollection() != null && !solrConfiguration.getCollection().isEmpty()) {
            builder.withDefaultCollection(solrConfiguration.getCollection());
        }

        if (solrConfiguration.getConnectionTimeout() != null || solrConfiguration.getIdleTimeout() != null) {
            Http2SolrClient.Builder internalClientBuilder = new Http2SolrClient.Builder();
            if (solrConfiguration.getConnectionTimeout() != null) {
                internalClientBuilder.withConnectionTimeout(solrConfiguration.getConnectionTimeout(), TimeUnit.MILLISECONDS);
            }
            if (solrConfiguration.getIdleTimeout() != null) {
                internalClientBuilder.withIdleTimeout(solrConfiguration.getIdleTimeout(), TimeUnit.MILLISECONDS);
            }

            builder.withInternalClientBuilder(internalClientBuilder);
        }
        CloudHttp2SolrClient cloudSolrClient = builder.build();
        return cloudSolrClient;
    }

}
