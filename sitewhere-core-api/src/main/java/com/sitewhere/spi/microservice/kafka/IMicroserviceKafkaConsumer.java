/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.spi.microservice.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.server.lifecycle.ITenantEngineLifecycleComponent;

/**
 * Component that consumes messages that are sent to a Kafka topic.
 * 
 * @author Derek
 */
public interface IMicroserviceKafkaConsumer extends ITenantEngineLifecycleComponent {

    /**
     * Get unique consumer id.
     * 
     * @return
     * @throws SiteWhereException
     */
    public String getConsumerId() throws SiteWhereException;

    /**
     * Get unique consumer group id.
     * 
     * @return
     * @throws SiteWhereException
     */
    public String getConsumerGroupId() throws SiteWhereException;

    /**
     * Get name of Kafka topics which will provide the messages.
     * 
     * @return
     * @throws SiteWhereException
     */
    public List<String> getSourceTopicNames() throws SiteWhereException;

    /**
     * Process a batch of records.
     * 
     * @param records
     * @throws SiteWhereException
     */
    public void processBatch(List<ConsumerRecord<String, byte[]>> records) throws SiteWhereException;
}