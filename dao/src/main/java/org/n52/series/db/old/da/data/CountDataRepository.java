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
package org.n52.series.db.old.da.data;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.count.CountValue;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.series.db.beans.CountDataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DataDao;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;

//@ValueAssemblerComponent(value = "count", datasetEntityType = DatasetEntity.class)
public class CountDataRepository extends AbstractNumericalDataRepository<CountDataEntity, CountValue, Integer> {

    public CountDataRepository(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected CountValue createEmptyValue() {
        return new CountValue();
    }

    @Override
    public CountValue getFirstValue(DatasetEntity entity, DbQuery query) {
        if (entity.getFirstQuantityValue() != null) {
            CountValue value = createEmptyValue();
            value.setValue(entity.getFirstQuantityValue().intValue());
            value.setTimestamp(createTimeOutput(entity.getFirstValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getFirstValue(entity, query);
    }

    @Override
    public CountValue getLastValue(DatasetEntity entity, DbQuery query) {
        if (entity.getLastQuantityValue() != null) {
            CountValue value = createEmptyValue();
            value.setValue(entity.getLastQuantityValue().intValue());
            value.setTimestamp(createTimeOutput(entity.getLastValueAt(), null, query.getParameters()));
            Locale locale = LocaleHelper.decode(query.getLocale());
            NumberFormat formatter = NumberFormat.getInstance(locale);
            value.setValueFormatter(formatter::format);
            return value;
        }
        return super.getLastValue(entity, query);
    }

    @Override
    protected Data<CountValue> assembleData(Long dataset, DbQuery query, Session session) {
        Data<CountValue> result = new Data<>();
        DataDao<CountDataEntity> dao = new DataDao<>(session);
        List<CountDataEntity> observations = dao.getAllInstancesFor(dataset, query);
        for (CountDataEntity observation : observations) {
            if (observation != null) {
                result.addNewValue(assembleDataValue(observation, observation.getDataset(), query));
            }
        }
        return result;
    }

    @Override
    public CountValue assembleDataValue(CountDataEntity observation, DatasetEntity series, DbQuery query) {
        if (observation == null) {
            // do not fail on empty observations
            return null;
        }

        ServiceEntity service = getServiceEntity(series);
        Integer observationValue = !service.isNoDataValue(observation) ? observation.getValue() : null;

        CountValue value = prepareValue(observation, query);
        value.setValue(observationValue);
        return value;
    }

}
