/*
 * Copyright (c) SiteWhere, LLC. All rights reserved. http://www.sitewhere.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.sitewhere.web.microservice;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.sitewhere.grpc.client.asset.AssetManagementApiDemux;
import com.sitewhere.grpc.client.batch.BatchManagementApiDemux;
import com.sitewhere.grpc.client.device.DeviceManagementApiDemux;
import com.sitewhere.grpc.client.event.DeviceEventManagementApiDemux;
import com.sitewhere.grpc.client.schedule.ScheduleManagementApiDemux;
import com.sitewhere.grpc.client.spi.ApiNotAvailableException;
import com.sitewhere.grpc.client.spi.client.IAssetManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.IBatchManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.IDeviceEventManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.IDeviceManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.IScheduleManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.ITenantManagementApiDemux;
import com.sitewhere.grpc.client.spi.client.IUserManagementApiDemux;
import com.sitewhere.grpc.client.tenant.TenantManagementApiDemux;
import com.sitewhere.grpc.client.user.UserManagementApiDemux;
import com.sitewhere.microservice.GlobalMicroservice;
import com.sitewhere.microservice.asset.AssetResolver;
import com.sitewhere.server.lifecycle.CompositeLifecycleStep;
import com.sitewhere.spi.SiteWhereException;
import com.sitewhere.spi.asset.IAssetResolver;
import com.sitewhere.spi.microservice.IMicroserviceIdentifiers;
import com.sitewhere.spi.server.lifecycle.ICompositeLifecycleStep;
import com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor;
import com.sitewhere.web.spi.microservice.IWebRestMicroservice;

/**
 * Microservice that provides web/REST functionality.
 * 
 * @author Derek
 */
public class WebRestMicroservice extends GlobalMicroservice implements IWebRestMicroservice {

    /** Static logger instance */
    private static Logger LOGGER = LogManager.getLogger();

    /** Microservice name */
    private static final String NAME = "Web/REST";

    /** Web/REST configuration file name */
    private static final String WEB_REST_CONFIGURATION = IMicroserviceIdentifiers.WEB_REST + ".xml";

    /** List of configuration paths required by microservice */
    private static final String[] CONFIGURATION_PATHS = { WEB_REST_CONFIGURATION };

    /** User management API demux */
    private IUserManagementApiDemux userManagementApiDemux;

    /** Tenant management API demux */
    private ITenantManagementApiDemux tenantManagementApiDemux;

    /** Device management API demux */
    private IDeviceManagementApiDemux deviceManagementApiDemux;

    /** Device event management API demux */
    private IDeviceEventManagementApiDemux deviceEventManagementApiDemux;

    /** Asset management API demux */
    private IAssetManagementApiDemux assetManagementApiDemux;

    /** Batch management API demux */
    private IBatchManagementApiDemux batchManagementApiDemux;

    /** Schedule management API demux */
    private IScheduleManagementApiDemux scheduleManagementApiDemux;

    /** Asset resolver */
    private IAssetResolver assetResolver;

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.microservice.spi.IMicroservice#getName()
     */
    @Override
    public String getName() {
	return NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.microservice.spi.IMicroservice#getIdentifier()
     */
    @Override
    public String getIdentifier() {
	return IMicroserviceIdentifiers.WEB_REST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.microservice.spi.IGlobalMicroservice#getConfigurationPaths( )
     */
    @Override
    public String[] getConfigurationPaths() throws SiteWhereException {
	return CONFIGURATION_PATHS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.microservice.spi.IGlobalMicroservice#
     * initializeFromSpringContexts(org.springframework.context. ApplicationContext,
     * java.util.Map)
     */
    @Override
    public void initializeFromSpringContexts(ApplicationContext global, Map<String, ApplicationContext> contexts)
	    throws SiteWhereException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.microservice.Microservice#afterMicroserviceStarted()
     */
    @Override
    public void afterMicroserviceStarted() {
	try {
	    waitForApisAvailable();
	    getLogger().info("All required APIs detected as available.");
	} catch (ApiNotAvailableException e) {
	    getLogger().error("Required APIs not available for web/REST.", e);
	}
    }

    /**
     * Wait for required APIs to become available.
     * 
     * @throws ApiNotAvailableException
     */
    protected void waitForApisAvailable() throws ApiNotAvailableException {
	getUserManagementApiDemux().waitForApiChannel().waitForApiAvailable();
	getLogger().info("User management API detected as available.");
	getTenantManagementApiDemux().waitForApiChannel().waitForApiAvailable();
	getLogger().info("Tenant management API detected as available.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.microservice.spi.IGlobalMicroservice#microserviceInitialize
     * (com.sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void microserviceInitialize(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Create GRPC components.
	createGrpcComponents();

	// Composite step for initializing microservice.
	ICompositeLifecycleStep init = new CompositeLifecycleStep("Initialize " + getName());

	// Initialize discoverable lifecycle components.
	init.addStep(initializeDiscoverableBeans(getWebRestApplicationContext(), monitor));

	// Initialize user management API demux.
	init.addInitializeStep(this, getUserManagementApiDemux(), true);

	// Initialize tenant management API demux.
	init.addInitializeStep(this, getTenantManagementApiDemux(), true);

	// Initialize device management API demux.
	init.addInitializeStep(this, getDeviceManagementApiDemux(), true);

	// Initialize device event management API demux.
	init.addInitializeStep(this, getDeviceEventManagementApiDemux(), true);

	// Initialize asset management API demux.
	init.addInitializeStep(this, getAssetManagementApiDemux(), true);

	// Initialize batch management API demux.
	init.addInitializeStep(this, getBatchManagementApiDemux(), true);

	// Initialize schedule management API demux.
	init.addInitializeStep(this, getScheduleManagementApiDemux(), true);

	// Execute initialization steps.
	init.execute(monitor);
    }

    /**
     * Create components that interact via GRPC.
     * 
     * @throws SiteWhereException
     */
    protected void createGrpcComponents() throws SiteWhereException {
	// User management.
	this.userManagementApiDemux = new UserManagementApiDemux(this);

	// Tenant management.
	this.tenantManagementApiDemux = new TenantManagementApiDemux(this);

	// Device management.
	this.deviceManagementApiDemux = new DeviceManagementApiDemux(this);

	// Device event management.
	this.deviceEventManagementApiDemux = new DeviceEventManagementApiDemux(this);

	// Asset management.
	this.assetManagementApiDemux = new AssetManagementApiDemux(this);
	this.assetResolver = new AssetResolver(getAssetManagementApiDemux());

	// Batch management.
	this.batchManagementApiDemux = new BatchManagementApiDemux(this);

	// Schedule management.
	this.scheduleManagementApiDemux = new ScheduleManagementApiDemux(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sitewhere.microservice.spi.IGlobalMicroservice#microserviceStart(com.
     * sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void microserviceStart(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Composite step for starting microservice.
	ICompositeLifecycleStep start = new CompositeLifecycleStep("Start " + getName());

	// Start discoverable lifecycle components.
	start.addStep(startDiscoverableBeans(getWebRestApplicationContext(), monitor));

	// Start user mangement API demux.
	start.addStartStep(this, getUserManagementApiDemux(), true);

	// Start tenant mangement API demux.
	start.addStartStep(this, getTenantManagementApiDemux(), true);

	// Start device mangement API demux.
	start.addStartStep(this, getDeviceManagementApiDemux(), true);

	// Start device event mangement API demux.
	start.addStartStep(this, getDeviceEventManagementApiDemux(), true);

	// Start asset mangement API demux.
	start.addStartStep(this, getAssetManagementApiDemux(), true);

	// Start batch mangement API demux.
	start.addStartStep(this, getBatchManagementApiDemux(), true);

	// Start schedule mangement API demux.
	start.addStartStep(this, getScheduleManagementApiDemux(), true);

	// Execute startup steps.
	start.execute(monitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.microservice.spi.IGlobalMicroservice#microserviceStop(com.
     * sitewhere.spi.server.lifecycle.ILifecycleProgressMonitor)
     */
    @Override
    public void microserviceStop(ILifecycleProgressMonitor monitor) throws SiteWhereException {
	// Composite step for stopping microservice.
	ICompositeLifecycleStep stop = new CompositeLifecycleStep("Stop " + getName());

	// Stop user mangement API demux.
	stop.addStopStep(this, getUserManagementApiDemux());

	// Stop tenant mangement API demux.
	stop.addStopStep(this, getTenantManagementApiDemux());

	// Stop device mangement API demux.
	stop.addStopStep(this, getDeviceManagementApiDemux());

	// Stop device event mangement API demux.
	stop.addStopStep(this, getDeviceEventManagementApiDemux());

	// Stop asset mangement API demux.
	stop.addStopStep(this, getAssetManagementApiDemux());

	// Stop batch mangement API demux.
	stop.addStopStep(this, getBatchManagementApiDemux());

	// Stop schedule mangement API demux.
	stop.addStopStep(this, getScheduleManagementApiDemux());

	// Stop discoverable lifecycle components.
	stop.addStep(stopDiscoverableBeans(getWebRestApplicationContext(), monitor));

	// Execute shutdown steps.
	stop.execute(monitor);
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getUserManagementApiDemux()
     */
    @Override
    public IUserManagementApiDemux getUserManagementApiDemux() {
	return userManagementApiDemux;
    }

    public void setUserManagementApiDemux(IUserManagementApiDemux userManagementApiDemux) {
	this.userManagementApiDemux = userManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getTenantManagementApiDemux()
     */
    @Override
    public ITenantManagementApiDemux getTenantManagementApiDemux() {
	return tenantManagementApiDemux;
    }

    public void setTenantManagementApiDemux(ITenantManagementApiDemux tenantManagementApiDemux) {
	this.tenantManagementApiDemux = tenantManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getDeviceManagementApiDemux()
     */
    @Override
    public IDeviceManagementApiDemux getDeviceManagementApiDemux() {
	return deviceManagementApiDemux;
    }

    public void setDeviceManagementApiDemux(IDeviceManagementApiDemux deviceManagementApiDemux) {
	this.deviceManagementApiDemux = deviceManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getDeviceEventManagementApiDemux()
     */
    @Override
    public IDeviceEventManagementApiDemux getDeviceEventManagementApiDemux() {
	return deviceEventManagementApiDemux;
    }

    public void setDeviceEventManagementApiDemux(IDeviceEventManagementApiDemux deviceEventManagementApiDemux) {
	this.deviceEventManagementApiDemux = deviceEventManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getAssetManagementApiDemux()
     */
    @Override
    public IAssetManagementApiDemux getAssetManagementApiDemux() {
	return assetManagementApiDemux;
    }

    public void setAssetManagementApiDemux(IAssetManagementApiDemux assetManagementApiDemux) {
	this.assetManagementApiDemux = assetManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getBatchManagementApiDemux()
     */
    @Override
    public IBatchManagementApiDemux getBatchManagementApiDemux() {
	return batchManagementApiDemux;
    }

    public void setBatchManagementApiDemux(IBatchManagementApiDemux batchManagementApiDemux) {
	this.batchManagementApiDemux = batchManagementApiDemux;
    }

    /*
     * @see com.sitewhere.web.spi.microservice.IWebRestMicroservice#
     * getScheduleManagementApiDemux()
     */
    @Override
    public IScheduleManagementApiDemux getScheduleManagementApiDemux() {
	return scheduleManagementApiDemux;
    }

    public void setScheduleManagementApiDemux(IScheduleManagementApiDemux scheduleManagementApiDemux) {
	this.scheduleManagementApiDemux = scheduleManagementApiDemux;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sitewhere.spi.server.lifecycle.ILifecycleComponent#getLogger()
     */
    @Override
    public Logger getLogger() {
	return LOGGER;
    }

    protected ApplicationContext getWebRestApplicationContext() {
	return getGlobalContexts().get(WEB_REST_CONFIGURATION);
    }

    public IAssetResolver getAssetResolver() {
	return assetResolver;
    }

    public void setAssetResolver(IAssetResolver assetResolver) {
	this.assetResolver = assetResolver;
    }
}