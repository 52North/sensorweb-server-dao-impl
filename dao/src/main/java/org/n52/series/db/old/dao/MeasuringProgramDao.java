/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.old.dao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Interval;
import org.n52.io.IntervalWithTimeZone;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.sensorweb.server.db.old.DataModelUtil;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.i18n.I18nMeasuringProgramEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasuringProgramDao extends AbstractDao<MeasuringProgramEntity>
        implements SearchableDao<MeasuringProgramEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasuringProgramDao.class);

    public MeasuringProgramDao(Session session) {
        super(session);
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<MeasuringProgramEntity> find(DbQuery q) {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return Collections.emptyList();
        }
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("find instance: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        return addFilters(criteria, query).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasuringProgramEntity> getAllInstances(DbQuery q) {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return Collections.emptyList();
        }
        DbQuery query = checkLevelParameterForHierarchyQuery(q);
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        return addFilters(criteria, query).list();
    }

    private Criteria addFilters(Criteria criteria, DbQuery query) {
        addTimespanTo(criteria, query.getParameters());
        Criteria datasetCriteria = criteria.createCriteria(MeasuringProgramEntity.PROPERTY_DATASETS);
        if (query.getParameters().getSpatialFilter() != null) {
            addSpatialFilter(query, datasetCriteria);
        }
        return query.addFilters(datasetCriteria, getDatasetProperty());
    }

    public Criteria addTimespanTo(Criteria criteria, IoParameters parameters) {
        if (parameters.containsParameter(Parameters.TIMESPAN)) {
            IntervalWithTimeZone timespan = parameters.getTimespan();
            Interval interval = timespan.toInterval();
            Date start = interval.getStart().toDate();
            Date end = interval.getEnd().toDate();
            criteria.add(Restrictions.or(
                    Restrictions.between(MeasuringProgramEntity.PROPERTY_MEASURING_TIME_START, start, end),
                    Restrictions.between(MeasuringProgramEntity.PROPERTY_MEASURING_TIME_END, start, end),
                    Restrictions.and(Restrictions.le(MeasuringProgramEntity.PROPERTY_MEASURING_TIME_START, start),
                            Restrictions.or(Restrictions.ge(MeasuringProgramEntity.PROPERTY_MEASURING_TIME_END, end),
                                    Restrictions.isNull(MeasuringProgramEntity.PROPERTY_MEASURING_TIME_END)))));
        }
        return criteria;
    }

    @Override
    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class<?> clazz) {
        Criteria criteria = session.createCriteria(clazz);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

    @Override
    protected MeasuringProgramEntity getInstance(String key, DbQuery query, Class<MeasuringProgramEntity> clazz) {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return null;
        }
        LOGGER.debug("get instance for '{}'. {}", key, query);
        Criteria criteria = session.createCriteria(clazz);
        return getInstance(key, query, clazz, criteria);
    }

    protected Class<I18nMeasuringProgramEntity> getI18NEntityClass() {
        return I18nMeasuringProgramEntity.class;
    }

    @Override
    protected Class<MeasuringProgramEntity> getEntityClass() {
        return MeasuringProgramEntity.class;
    }

    @Override
    protected String getDatasetProperty() {
        // TODO Auto-generated method stub
        return "";
    }

}
