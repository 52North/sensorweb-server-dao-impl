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

package org.n52.series.db.assembler;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.profile.ProfileDataItem;
import org.n52.io.response.dataset.profile.ProfileValue;
import org.n52.series.db.DataRepository;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.ProfileDatasetEntity;
import org.n52.series.db.old.dao.DbQuery;

public abstract class ProfileValueAssembler<P extends ProfileDatasetEntity, V, T>
        extends
        AbstractValueAssembler<P, ProfileDataEntity, ProfileValue<V>, Set<DataEntity< ? >>> {

    private static final String PARAMETER_NAME = "name";
    private static final String PARAMETER_VALUE = "value";
    private static final String PARAMETER_UNIT = "unit";

    public ProfileValueAssembler(DataRepository<ProfileDataEntity> profileDataRepository,
                                 DatasetRepository<P> datasetRepository) {
        super(profileDataRepository, datasetRepository);
    }

    @Override
    protected Data<ProfileValue<V>> assembleDataValues(P dataset, DbQuery query) {
        query.setComplexParent(true);
        return super.assembleDataValues(dataset, query);
    }

    @Override
    public abstract ProfileValue<V> assembleDataValue(ProfileDataEntity observation, P dataset, DbQuery query);

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            ProfileDataEntity parent,
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
        if (parent.hasVerticalUnit()) {
            dataItem.setVerticalUnit(parent.getVerticalUnit()
                                           .getIdentifier());
        }
        return dataItem;
    }

    protected <E extends DataEntity<T>> ProfileDataItem<T> assembleDataItem(E dataEntity,
                                                                            ProfileValue<T> profile,
                                                                            Set<Map<String, Object>> parameters,
                                                                            P dataset,
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

        if ((profile.getVerticalUnit() == null)
                || !profile.getVerticalUnit()
                           .equals(verticalUnit)) {
            dataItem.setVerticalUnit(verticalUnit);
        }
        return dataItem;
    }

    private String getVerticalUnit(Set<Map<String, Object>> parameters, ProfileDatasetEntity dataset) {
        String unit = null;
        for (Map<String, Object> parameter : parameters) {
            if ((unit == null)
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

    private BigDecimal getVerticalValue(Set<Map<String, Object>> parameters, String verticalName) {
        for (Map<String, Object> parameterObject : parameters) {
            if (isVertical(parameterObject, verticalName)) {
                return (BigDecimal) parameterObject.get(PARAMETER_VALUE);
            }
        }
        return null;
    }

    private boolean isVertical(Map<String, Object> parameterObject, String verticalName) {
        if (parameterObject.containsKey(PARAMETER_NAME)) {
            String value = (String) parameterObject.get(PARAMETER_NAME);
            return value.equalsIgnoreCase(verticalName);
        }
        return false;
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
