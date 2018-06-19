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
package org.n52.series.db.da.data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.series.db.HibernateSessionStore;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.dao.DataDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DbQueryFactory;

public abstract class ProfileDataRepository<T, P extends ProfileDatasetEntity>
        extends AbstractDataRepository<P, ProfileDataEntity, ProfileValue<T>> {

    private static final String PARAMETER_NAME = "name";
    private static final String PARAMETER_VALUE = "value";
    private static final String PARAMETER_UNIT = "unit";

    public ProfileDataRepository(HibernateSessionStore sessionStore,
                                 DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    @Override
    protected ProfileValue<T> createEmptyValue() {
        return new ProfileValue<>();
    }

    @Override
    public ProfileValue<T> getFirstValue(P dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getFirstValue(dataset, session, query);
    }

    @Override
    public ProfileValue<T> getLastValue(P dataset, Session session, DbQuery query) {
        query.setComplexParent(true);
        return super.getLastValue(dataset, session, query);
    }

    protected boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        if (parameterObject.containsKey(PARAMETER_NAME)) {
            String value = (String) parameterObject.get(PARAMETER_NAME);
            return value.equalsIgnoreCase(verticalName);
        }
        return false;
    }

    @Override
    protected Data<ProfileValue<T>> assembleData(P datasetEntity, DbQuery query, Session session) {
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
                                                   P dataset,
                                                   DbQuery query) {
        ProfileValue<T> profile = createValue(observation, dataset, query);
        return addMetadatasIfNeeded(observation, profile, dataset, query);
    }

    protected abstract ProfileValue<T> createValue(ProfileDataEntity observation,
                                                   ProfileDatasetEntity dataset,
                                                   DbQuery query);

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            ProfileDataEntity observation,
                                                                            DbQuery query) {
        ProfileDataItem<T> dataItem = new ProfileDataItem<>();
        dataItem.setValue(dataEntity.getValue());

        IoParameters parameters = query.getParameters();
        BigDecimal verticalTo = dataEntity.getVerticalTo();
        BigDecimal verticalFrom = dataEntity.getVerticalFrom();

        dataItem.setVertical(verticalTo);
        if (parameters.isShowVerticalIntervals() && dataEntity.hasVerticalFrom()) {
            dataItem.setVerticalFrom(verticalFrom);
        }
        if (observation.hasVerticalUnit()) {
            dataItem.setVerticalUnit(observation.getVerticalUnit()
                                                .getIdentifier());
        }
        return dataItem;
    }

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            Set<Map<String, Object>> parameters,
                                                                            ProfileDatasetEntity dataset,
                                                                            DbQuery query) {
        ProfileDataItem<T> dataItem = new ProfileDataItem<>();
        dataItem.setValue(dataEntity.getValue());
        String verticalUnit = getVerticalUnit(parameters, dataset);

        if (getParameterNames(parameters).contains(dataset.getVerticalParameterName())) {
            dataItem.setVertical(getVerticalValue(parameters, dataset.getVerticalParameterName()));
        } else if (getParameterNames(parameters).contains(dataset.getVerticalFromParameterName())
                && getParameterNames(parameters).contains(dataset.getVerticalToParameterName())) {
            dataItem.setVertical(getVerticalValue(parameters, dataset.getVerticalToParameterName()));
            BigDecimal verticalFrom = getVerticalValue(parameters, dataset.getVerticalFromParameterName());
            boolean showVerticalIntervals = query.getParameters().isShowVerticalIntervals();
            if (showVerticalIntervals && dataEntity.hasVerticalFrom()) {
                dataItem.setVerticalFrom(verticalFrom);
            }
        }

        if (profile.getVerticalUnit() == null
                || !profile.getVerticalUnit()
                           .equals(verticalUnit)) {
            dataItem.setVerticalUnit(verticalUnit);
        }
        return dataItem;
    }

    private BigDecimal getVerticalValue(Set<Map<String, Object>> parameters, String verticalName) {
        for (Map<String, Object> parameterObject : parameters) {
            if (isVertical(parameterObject, verticalName)) {
                return (BigDecimal) parameterObject.get(PARAMETER_VALUE);
            }
        }
        return null;
    }

    private String getVerticalUnit(Set<Map<String, Object>> parameters, ProfileDatasetEntity dataset) {
        String unit = null;
        for (Map<String, Object> parameter : parameters) {
            if (unit == null
                    && parameter.containsKey(PARAMETER_NAME)
                    &&
                    (parameter.get(PARAMETER_NAME)
                              .equals(dataset.getVerticalParameterName())
                            || parameter.get(PARAMETER_NAME)
                                        .equals(dataset.getVerticalFromParameterName())
                            || parameter.get(PARAMETER_NAME)
                                        .equals(dataset.getVerticalToParameterName()))) {
                unit = (String) parameter.get(PARAMETER_UNIT);
            }
        }
        return unit;
    }

    private Set<String> getParameterNames(Set<Map<String, Object>> parameters) {
        Set<String> names = new HashSet<>();
        for (Map<String, Object> parameter : parameters) {
            if (parameter.containsKey(PARAMETER_NAME)) {
                names.add((String) parameter.get(PARAMETER_NAME));
            }
        }
        return names;
    }

}
