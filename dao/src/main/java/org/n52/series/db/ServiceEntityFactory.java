/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.db;

import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.series.db.beans.ServiceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Configurable
@Component
public class ServiceEntityFactory implements Constructable {

    private static final String SERVICE_ID_KEY = "helgoland.service.id";
    private static final String SERVICE_NAME_KEY = "helgoland.service.name";
    private static final String SERVICE_VERSION_KEY = "helgoland.service.version";
    private static final String SERVICE_NO_DATA_VALUES_KEY = "helgoland.service.nodatavalues";
    private static final Integer DEFAULT_ID = 1;
    private static final String DEFAULT_NAME = "My RESTful Dataset Service";
    private static final String DEFAULT_VERSION = "3.0";
    private static final String DEFAULT_NO_DATA_VALUES = "-9999.0,99999,NO_DATA";

    // via xml or db
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;
    private Integer id;
    private String name;
    private String version;
    private String noDataValues;
    private boolean initialized;

    public ServiceEntity getServiceEntity() {
        return serviceEntity;
    }

    @Override
    public void init() {
        ServiceEntity createdService = createServiceEntity();
        if (serviceEntity == null) {
            serviceEntity = getDefaultServiceEntity();
        }
        if (!serviceEntity.equals(createdService)) {
            serviceEntity = createdService;
        }
        initialized = true;
    }

    @Setting(SERVICE_ID_KEY)
    public void setId(Integer id) {
        this.id = id;
        updateEntity();
    }

    @Setting(SERVICE_NAME_KEY)
    public void setName(String name) {
        this.name = name;
        updateEntity();
    }

    @Setting(SERVICE_VERSION_KEY)
    public void setVersion(String version) {
        this.version = version;
        updateEntity();
    }

    @Setting(SERVICE_NO_DATA_VALUES_KEY)
    public void setNoDataValues(String noDataValues) {
        this.noDataValues = noDataValues;
        updateEntity();
    }

    private boolean check(String check) {
        return check != null && !check.isEmpty();
    }

    private ServiceEntity createServiceEntity() {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(Long.valueOf(id != null ? id : DEFAULT_ID));
        entity.setName(check(name) ? name : DEFAULT_NAME);
        entity.setVersion(check(version) ? version : DEFAULT_VERSION);
        entity.setNoDataValues(noDataValues);
        return entity;
    }

    private void updateEntity() {
        if (initialized) {
            serviceEntity.setId(Long.valueOf(id));
            serviceEntity.setName(name);
            serviceEntity.setVersion(version);
            serviceEntity.setNoDataValues(noDataValues);
        }
    }

    private ServiceEntity getDefaultServiceEntity() {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(Long.valueOf(DEFAULT_ID));
        entity.setName(DEFAULT_NAME);
        entity.setVersion(DEFAULT_VERSION);
        entity.setNoDataValues(DEFAULT_NO_DATA_VALUES);
        return entity;
    }

}
