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

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.category.CategoryValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.CategoryDatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.data.Data.CategoryData;
import org.n52.series.db.beans.dataset.CategoryDataset;
import org.n52.series.db.beans.ereporting.EReportingCategoryDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class CategoryDataRepository
        extends AbstractDataRepository<CategoryDataset, CategoryData, CategoryValue> {

    @Override
    public Class<?> getDatasetEntityType(Session session) {
        return DataModelUtil.isEntitySupported(EReportingCategoryDatasetEntity.class, session)
                ? EReportingCategoryDatasetEntity.class
                : CategoryDatasetEntity.class;
    }

    @Override
    protected Data<CategoryValue> assembleData(CategoryDataset seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<CategoryValue> result = new Data<>();
        DataDao<CategoryData> dao = new DataDao<>(session);
        List<CategoryData> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (CategoryData observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    @Override
    protected CategoryValue createEmptyValue() {
        return new CategoryValue();
    }

    @Override
    public CategoryValue createSeriesValueFor(CategoryData observation,
                                              CategoryDataset series,
                                              DbQuery query) {
        ServiceEntity service = getServiceEntity(series);
        String observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        CategoryValue value = createValue(observation, series, query, observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

    private CategoryValue createValue(CategoryData observation,
                                      CategoryDataset series,
                                      DbQuery query,
                                      String observationValue) {
        ServiceEntity service = getServiceEntity(series);
        String textValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;
        return createValue(textValue, observation, query);
    }

    CategoryValue createValue(String observationValue,
                              CategoryData observation,
                              DbQuery query) {
        Date timeend = observation.getSamplingTimeEnd();
        Date timestart = observation.getSamplingTimeStart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        return parameters.isShowTimeIntervals()
                ? new CategoryValue(start, end, observationValue)
                : new CategoryValue(end, observationValue);
    }

}
