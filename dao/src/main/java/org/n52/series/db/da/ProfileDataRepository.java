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
import java.util.Map;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;

public abstract class ProfileDataRepository<T>
        extends AbstractDataRepository<ProfileDatasetEntity, ProfileDataEntity, ProfileValue<T>> {

    @Override
    protected ProfileValue<T> createEmptyValue() {
        return new ProfileValue<>();
    }

    @Override
    public ProfileValue<T> getFirstValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getFirstValue(dataset, session, query);
    }

    @Override
    public ProfileValue<T> getLastValue(ProfileDatasetEntity dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getLastValue(dataset, session, query);
    }

    @Override
    public Class<ProfileDatasetEntity> getDatasetEntityType() {
        return ProfileDatasetEntity.class;
    }

    protected boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        String parameter = "name";
        if (parameterObject.containsKey(parameter)) {
            String value = (String) parameterObject.get(parameter);
            return value.equalsIgnoreCase(verticalName);
        }
        return false;
    }

    @Override
    protected Data<ProfileValue<T>> assembleData(ProfileDatasetEntity datasetEntity, DbQuery query, Session session)
            throws DataAccessException {
        query.setComplexParent(true);
        Data<ProfileValue<T>> result = new Data<>();
        DataDao<ProfileDataEntity> dao = createDataDao(session);
        List<ProfileDataEntity> observations = dao.getAllInstancesFor(datasetEntity, query);
        for (ProfileDataEntity observation : observations) {
            if (observation != null) {
                result.addValues(createSeriesValueFor(observation, datasetEntity, query));
            }
        }
        return result;
    }

    @Override
    protected Data<ProfileValue<T>> assembleDataWithReferenceValues(ProfileDatasetEntity datasetEntity,
                                                                    DbQuery dbQuery,
                                                                    Session session)
            throws DataAccessException {

        // TODO handle reference values

        return assembleData(datasetEntity, dbQuery, session);
    }

    protected ProfileValue<T> createProfileValue(ProfileDataEntity observation, DbQuery query) {
        Date timeend = observation.getSamplingTimeEnd();
        Date timestart = observation.getSamplingTimeStart();
        long end = timeend.getTime();
        long start = timestart.getTime();
        IoParameters parameters = query.getParameters();
        ProfileValue<T> profile = parameters.isShowTimeIntervals()
                ? new ProfileValue<>(start, end, null)
                : new ProfileValue<>(end, null);
        return profile;
    }

    @Override
    protected ProfileValue<T> createSeriesValueFor(ProfileDataEntity observation,
                                                   ProfileDatasetEntity dataset,
                                                   DbQuery query) {
        ProfileValue<T> profile = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, profile, dataset, query);
    }

    protected abstract ProfileValue<T> createValue(ProfileDataEntity observation,
                                                   ProfileDatasetEntity dataset,
                                                   DbQuery query);

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E quantityEntity,
                                                                            ProfileValue<T> profile,
                                                                            Map<String, Object> parameterObject) {
        ProfileDataItem<T> dataItem = new ProfileDataItem<>();
        dataItem.setValue(quantityEntity.getValue());
        // set vertical's value
        dataItem.setVertical((Double) parameterObject.get("value"));
        String verticalUnit = (String) parameterObject.get("unit");
        if (profile.getVerticalUnit() == null) {
            profile.setVerticalUnit(verticalUnit);
        }
        if (profile.getVerticalUnit() == null
                || !profile.getVerticalUnit()
                           .equals(verticalUnit)) {
            dataItem.setVerticalUnit(verticalUnit);
        }
        return dataItem;
    }

}
