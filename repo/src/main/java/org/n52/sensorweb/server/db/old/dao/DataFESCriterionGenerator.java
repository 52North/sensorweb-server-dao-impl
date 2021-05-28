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

import java.util.Optional;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MoreRestrictions;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.shetland.ogc.filter.ComparisonFilter;
import org.n52.shetland.ogc.filter.Filter;
import org.n52.shetland.ogc.filter.SpatialFilter;

/**
 * Class to create a {@linkplain Criterion criterion} for {@linkplain DataEntity data entities} from an FES
 * {@linkplain Filter filter}.
 *
 * @author Christian Autermann
 */
public class DataFESCriterionGenerator extends FESCriterionGenerator {

    /**
     * Creates a new {@code DataFESCriterionGenerator}.
     *
     * @param criteria
     *        the criteria
     * @param unsupportedIsTrue
     *        if the generator encounters a filter expression it could not translate it may generate a
     *        criterion that is always {@code true} or always {@code false} depending on this flag
     * @param matchDomainIds
     *        if filter on observation parameters like feature, offering or procedure should match on
     *        their respective domain identifiers or on the primary keys in the database
     * @param complexParent
     *        if the queries should result in the parent observation and hide the child observations
     */
    public DataFESCriterionGenerator(Criteria criteria,
                                     boolean unsupportedIsTrue,
                                     boolean matchDomainIds,
                                     boolean complexParent) {
        super(criteria, unsupportedIsTrue, matchDomainIds, complexParent);
    }

    @Override
    protected Criterion createResultCriterion(ComparisonFilter filter) {
        return getResultSubqueries(filter)
                                          // just get the ID
                                          .map(q -> q.setProjection(Projections.property(DataEntity.PROPERTY_ID)))
                                          // create a property IN expression for each query
                                          .map(q -> Subqueries.propertyIn(DataEntity.PROPERTY_ID, q))
                                          // and wrap everything into a disjunction
                                          .collect(MoreRestrictions.toDisjunction());
    }

    @Override
    protected Criterion createDataCriterion(Criterion criterion) {
        return criterion;
    }

    @Override
    protected Criterion createDatasetCriterion(String property, ComparisonFilter filter) {

        Object value;
        if (isMatchDomainIds()) {
            filter.setValueReference(DescribableEntity.PROPERTY_DOMAIN_ID);
            value = filter.getValue();
        } else {
            filter.setValueReference(DescribableEntity.PROPERTY_ID);
            Optional<Long> id = parseLong(filter.getValue());
            if (!id.isPresent()) {
                return unsupported(filter);
            }
            value = id.get();
        }

        return createDatasetCriterion(property, createComparison(filter, value));
    }

    @Override
    protected Criterion createDatasetCriterion(String property, SpatialFilter filter) {
        return createDatasetCriterion(property, createSpatialFilterCriterion(filter));
    }

    /**
     * Return a criterion for the {@linkplain DataEntity data entity} that applies {@code criterion} to the
     * supplied
     * {@code property} of the {@linkplain DatasetEntity dataset}.
     *
     * @param property
     *        the property of the dataset
     * @param criterion
     *        the criterion to apply
     * @return the data entity criterion
     */
    private Criterion createDatasetCriterion(String property, Criterion criterion) {
        DetachedCriteria subquery = DetachedCriteria.forClass(DatasetEntity.class)
                                                    .setProjection(Property.forName(DatasetEntity.PROPERTY_ID))
                                                    .add(Restrictions.eq(DatasetEntity.PROPERTY_DELETED, Boolean.FALSE))
                                                    .add(Restrictions.eq(DatasetEntity.PROPERTY_PUBLISHED,
                                                                         Boolean.TRUE))
                                                    .createCriteria(property)
                                                    .add(criterion);

        return Subqueries.propertyIn(DataEntity.PROPERTY_DATASET, subquery);
    }
}
