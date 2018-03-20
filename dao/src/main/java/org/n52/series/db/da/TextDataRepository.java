/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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

import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.text.TextValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.TextDataEntity;
import org.n52.series.db.beans.TextDatasetEntity;
import org.n52.series.db.beans.data.Data.TextData;
import org.n52.series.db.beans.dataset.TextDataset;
import org.n52.series.db.beans.ereporting.EReportingTextDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public class TextDataRepository extends AbstractDataRepository<TextDataset, TextData, TextValue> {

    @Override
    public Class<?> getDatasetEntityType(Session session) {
        return DataModelUtil.isEntitySupported(EReportingTextDatasetEntity.class, session)
                ? EReportingTextDatasetEntity.class
                : TextDatasetEntity.class;
    }

    @Override
    protected Data<TextValue> assembleData(TextDataset seriesEntity, DbQuery query, Session session)
            throws DataAccessException {
        Data<TextValue> result = new Data<>();
        DataDao<TextData> dao = new DataDao<>(session);
        List<TextData> observations = dao.getAllInstancesFor(seriesEntity, query);
        for (TextData observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, seriesEntity, query));
            }
        }
        return result;
    }

    @Override
    protected TextValue createEmptyValue() {
        return new TextValue();
    }

    @Override
    public TextValue createSeriesValueFor(TextData observation, TextDataset series, DbQuery query) {
        if (observation == null) {
            // do not fail on empty observations
            return null;
        }

        ServiceEntity service = getServiceEntity(series);
        String observationValue = !service.isNoDataValue(observation)
                ? observation.getValue()
                : null;

        TextValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return addMetadatasIfNeeded(observation, value, series, query);
    }

    TextValue createValue(String observationValue,
                          TextDataEntity observation,
                          DbQuery query) {
        TextValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return value;
    }

}
