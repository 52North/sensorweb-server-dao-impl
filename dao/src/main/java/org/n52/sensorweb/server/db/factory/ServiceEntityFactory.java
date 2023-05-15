/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.factory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.series.db.beans.ServiceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Configurable
@Component
@SuppressFBWarnings({ "EI_EXPOSE_REP" })
public class ServiceEntityFactory implements Constructable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEntityFactory.class);

    private static final String SERVICE_ID_KEY = "helgoland.service.id";
    private static final String SERVICE_NAME_KEY = "helgoland.service.name";
    private static final String SERVICE_VERSION_KEY = "helgoland.service.version";
    private static final String SERVICE_NO_DATA_VALUES_KEY = "helgoland.service.nodatavalues";
    private static final Integer DEFAULT_ID = 1;
    private static final String DEFAULT_NAME = "My RESTful Dataset Service (default)";
    private static final String DEFAULT_VERSION = "3.0";
    private static final String DEFAULT_NO_DATA_VALUES = "-9999.0,99999,NO_DATA";

    // via xml or db
    @Autowired(required = false)
    protected ServiceEntity serviceEntity;
    private Integer id;
    private String name;
    private String version;
    private String noDataValues;
    private Collection<String> noDataValuesList = new LinkedList<>();
    private Collection<BigDecimal> quantityNoDataValues = new LinkedList<>();
    private Collection<Integer> countNoDataValues = new LinkedList<>();
    private boolean initialized;

    public ServiceEntity getServiceEntity() {
        return serviceEntity;
    }

    public ServiceEntity getServiceEntity(ServiceEntity entity) {
        return entity != null ? enrich(entity) : serviceEntity;
    }

    @Override
    public void init() {
        if (serviceEntity == null) {
            serviceEntity = createServiceEntity();
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
        LOGGER.debug("Set noData values: {}", noDataValues);
        this.noDataValuesList.clear();
        this.countNoDataValues.clear();
        this.quantityNoDataValues.clear();
        if (noDataValues != null && !noDataValues.isEmpty()) {
            final String[] values = noDataValues.split(",");
            this.noDataValuesList.addAll(Arrays.asList(values));
            convertToBigDecimal(this.noDataValuesList);
            convertToIntegers(this.noDataValuesList);
        }
        updateEntity();
    }

    private void convertToBigDecimal(Collection<String> collection) {
        for (String value : collection) {
            String trimmed = value.trim();
            try {
                this.quantityNoDataValues.add(new BigDecimal(trimmed));
            } catch (NumberFormatException e) {
                LOGGER.trace("Ignoring NO_DATA value {} (not a big decimal value).", trimmed);
            }
        }
    }

    private void convertToIntegers(Collection<String> collection) {
        for (String value : collection) {
            String trimmed = value.trim();
            try {
                this.countNoDataValues.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                LOGGER.trace("Ignoring NO_DATA value {} (not an integer).", trimmed);
            }
        }
    }

    private boolean check(String check) {
        return check != null && !check.isEmpty();
    }

    private ServiceEntity createServiceEntity() {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(Long.valueOf(id != null ? id : DEFAULT_ID));
        entity.setName(check(name) ? name : DEFAULT_NAME);
        entity.setVersion(check(version) ? version : DEFAULT_VERSION);
        entity.setNoDataValues(check(noDataValues) ? noDataValues : DEFAULT_NO_DATA_VALUES);
        addIdentifier(entity);
        return entity;
    }

    private void updateEntity() {
        if (initialized) {
            serviceEntity.setId(Long.valueOf(id));
            serviceEntity.setName(name);
            serviceEntity.setVersion(version);
            serviceEntity.setNoDataValues(noDataValuesList, quantityNoDataValues, countNoDataValues);
            addIdentifier(serviceEntity);
        }
    }

    private ServiceEntity enrich(ServiceEntity entity) {
        entity.setNoDataValues(noDataValuesList, quantityNoDataValues, countNoDataValues);
        return entity;
    }

    private void addIdentifier(ServiceEntity entity) {
        entity.setIdentifier(Long.toString(entity.getId()));
    }

}
