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
package org.n52.sensorweb.server.db.old.dao;

import java.util.Objects;

import org.n52.faroe.annotation.Configurable;
import org.n52.faroe.annotation.Setting;
import org.n52.io.request.IoParameters;

@Configurable
public class DefaultDbQueryFactory implements DbQueryFactory {

    private static final String STORAGE_EPSG_KEY = "service.defaultEpsg";
    private static final String EPSG_PREFIX = "EPSG:";
    private String databaseSrid = "EPSG:4326";
    private Integer epsgCode;

    public DefaultDbQueryFactory() {
    }

    public DefaultDbQueryFactory(String srid) {
        Objects.requireNonNull(srid, "srid is null");
        this.databaseSrid = srid;
    }

    @Override
    public DbQuery createFrom(IoParameters parameters) {
        DbQuery query = new DbQuery(parameters);
        query.setDatabaseSridCode(getDatabaseSrid());
        return query;
    }

    @Override
    public String getDatabaseSrid() {
        return epsgCode != null && epsgCode > 0 ? EPSG_PREFIX.concat(epsgCode.toString()) : databaseSrid;
    }

    @Override
    public void setDatabaseSrid(String databaseSrid) {
        if (databaseSrid != null && !databaseSrid.isEmpty()) {
            this.databaseSrid = databaseSrid.startsWith(EPSG_PREFIX) ? databaseSrid : EPSG_PREFIX.concat(databaseSrid);
        }
    }

    @Setting(STORAGE_EPSG_KEY)
    public void setStorageEpsg(int epsgCode) {
        this.epsgCode = epsgCode;
    }

}
