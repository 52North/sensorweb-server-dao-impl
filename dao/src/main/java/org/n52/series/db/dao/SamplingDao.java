/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.dao;

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
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.i18n.I18nSamplingEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamplingDao extends AbstractDao<SamplingEntity> implements SearchableDao<SamplingEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamplingDao.class);

    public SamplingDao(Session session) {
        super(session);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SamplingEntity> find(DbQuery query) {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return Collections.emptyList();
        }
        LOGGER.debug("find instance: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        criteria.add(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, "%" + query.getSearchTerm() + "%"));
        return addFilters(criteria, query).list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SamplingEntity> getAllInstances(DbQuery query) throws DataAccessException {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return Collections.emptyList();
        }
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = getDefaultCriteria(query);
        criteria = i18n(getI18NEntityClass(), criteria, query);
        return addFilters(criteria, query).list();
    }

    private Criteria addFilters(Criteria criteria, DbQuery query) {
        addMonitoringProgramFilter(query, criteria);
        addTimespanTo(criteria, query.getParameters());
        Criteria datasetCriteria = criteria.createCriteria(SamplingEntity.PROPERTY_DATASETS);
        if (query.getParameters().getSpatialFilter() != null) {
            addSpatialFilter(query, datasetCriteria);
        }
        return query.addFilters(datasetCriteria, getDatasetProperty());
    }

    private void addMonitoringProgramFilter(DbQuery query, Criteria criteria) {
        if (query.getParameters().containsParameter(IoParameters.MEASURING_PROGRAMS)) {
            criteria.createCriteria(SamplingEntity.PROPERTY_MEASURING_PROGRAM)
                    .add(query.createIdFilter(query.getParameters().getMeasuringPrograms(), null));
        }
    }

    public Criteria addTimespanTo(Criteria criteria, IoParameters parameters) {
        if (parameters.containsParameter(Parameters.TIMESPAN)) {
            IntervalWithTimeZone timespan = parameters.getTimespan();
            Interval interval = timespan.toInterval();
            Date start = interval.getStart().toDate();
            Date end = interval.getEnd().toDate();
            criteria.add(Restrictions.or(Restrictions.between(SamplingEntity.PROPERTY_SAMPLING_TIME_START, start, end),
                    Restrictions.between(SamplingEntity.PROPERTY_SAMPLING_TIME_END, start, end),
                    Restrictions.and(Restrictions.le(SamplingEntity.PROPERTY_SAMPLING_TIME_START, start),
                            Restrictions.ge(SamplingEntity.PROPERTY_SAMPLING_TIME_END, end))));
        }
        return criteria;
    }

    @Override
    protected SamplingEntity getInstance(String key, DbQuery query, Class<SamplingEntity> clazz) {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return null;
        }
        LOGGER.debug("get instance for '{}'. {}", key, query);
        Criteria criteria = session.createCriteria(clazz);
        return getInstance(key, query, clazz, criteria);
    }

    @Override
    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class<?> clazz) {
//      String nonNullAlias = alias != null ? alias : getDefaultAlias();
//      Criteria criteria = session.createCriteria(clazz, nonNullAlias);
        Criteria criteria = session.createCriteria(clazz);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

    protected Class<I18nSamplingEntity> getI18NEntityClass() {
        return I18nSamplingEntity.class;
    }

    @Override
    protected Class<SamplingEntity> getEntityClass() {
        return SamplingEntity.class;
    }

    @Override
    protected String getDatasetProperty() {
        return "";
    }

}
