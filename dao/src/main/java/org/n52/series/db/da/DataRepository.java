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
package org.n52.series.db.da;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.dao.DbQuery;

public interface DataRepository<S extends DatasetEntity, E extends DataEntity<T>, V extends AbstractValue<?>, T> {

    /**
     * Assembles observation values as {@link Data} output.
     *
     * @param id
     *            the dataset id
     * @param query
     *            the query
     * @return the assembled data
     */
    Data<V> getData(String id, DbQuery query);

    /**
     * Assembles a list of reference values.
     *
     * @param datasetEntity
     *            the dataset
     * @param query
     *            the query
     * @param session
     *            hibernate session
     * @return a list of reference values
     */
    default List<ReferenceValueOutput<V>> getReferenceValues(S datasetEntity, DbQuery query, Session session) {
        return Collections.emptyList();
    }

    /**
     * Assembles an output for a data entity containing all metadata (geometry, parameters, valid time, etc.)
     * for a given query.
     *
     * @param dataEntity
     *            the single data entity to assemble
     * @param datasetEntity
     *            the dataset the data entity belongs to
     * @param query
     *            the query
     * @return the assembled output
     */
    V assembleDataValueWithMetadata(E dataEntity, S datasetEntity, DbQuery query);

    /**
     * Assembles an output for a data entity for a given query.
     *
     * @param dataEntity
     *            the single data entity to assemble
     * @param datasetEntity
     *            the dataset the data entity belongs to
     * @param query
     *            the query
     * @return the assembled output
     */
    V assembleDataValue(E dataEntity, S datasetEntity, DbQuery query);

    // /**
    // * Assembles plain output containing date/time and the as-is value for a given query.
    // *
    // * @param value
    // * the value
    // * @param data
    // * the data entity
    // * @param query
    // * the query
    // * @return the assembled output
    // */
    // V assembleDataValue(T value, E data, DbQuery query);

    /**
     * @param entity
     *            the dataset entity
     * @param session
     *            the sesssion
     * @param query
     *            the query
     * @return the first value for the given dataset
     */
    V getFirstValue(S entity, Session session, DbQuery query);

    /**
     * @param entity
     *            the dataset entity
     * @param session
     *            the session
     * @param query
     *            the query
     * @return the last value for the given dataset
     */
    V getLastValue(S entity, Session session, DbQuery query);

    GeometryEntity getLastKnownGeometry(DatasetEntity entity, Session session, DbQuery query);

    /**
     * Finds the closest value before a given timespan.
     *
     * @param dataset
     *            the dataset
     * @param query
     *            the query containing the timespan
     * @return the closest value before a given timespan
     */
    E getClosestValueBeforeStart(S dataset, DbQuery query);

    /**
     * Finds the closest value after a given timespan.
     *
     * @param dataset
     *            the dataset
     * @param query
     *            the query containing the timespan
     * @return the closest value after a given timespan
     */
    E getClosestValueAfterEnd(S dataset, DbQuery query);

}
