/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
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

package org.n52.series.db.da;

import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.dao.DbQuery;

public class EntityCounterWrapper extends EntityCounter {

    public Integer countFeatures(IoParameters queryMap) {
        try {
            return super.countFeatures(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countOfferings(IoParameters queryMap) {
        // offerings equals procedures in our case
        return countProcedures(queryMap);
    }

    public Integer countProcedures(IoParameters queryMap) {
        try {
            return super.countProcedures(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countServices(IoParameters queryMap) {
        try {
            return super.countServices(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countPhenomena(IoParameters queryMap) {
        try {
            return super.countPhenomena(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countCategories(IoParameters queryMap) {
        try {
            return super.countCategories(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countPlatforms(IoParameters queryMap) {
        try {
            return super.countPlatforms(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }    
    }

    public Integer countDatasets(IoParameters queryMap) {
        try {
            return super.countDatasets(new DbQuery(queryMap));
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countStations() {
        try {
            return super.countStations();
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public Integer countTimeseries() {
        try {
            return super.countTimeseries();
        } catch (DataAccessException ex) {
            return null;
        }
    }
}
