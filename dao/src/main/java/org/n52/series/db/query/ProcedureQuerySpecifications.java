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
package org.n52.series.db.query;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QProcedureEntity;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class ProcedureQuerySpecifications extends ParameterQuerySpecifications {

	public static ProcedureQuerySpecifications of(DbQuery dbQuery) {
        return new ProcedureQuerySpecifications(dbQuery);
    }

    private ProcedureQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches procedures included in a result of a given subquery, i.e.
     *
     * <pre>
     *   where id in (select fk_procedure_id from dataset where &lt;subquery&gt;)
     * </pre>
     *
     * @param subquery
     *        the query
     * @return a boolean expression
     */
    public BooleanExpression selectFrom(JPQLQuery<DatasetEntity> subquery) {
        QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        QProcedureEntity procedureentity = QProcedureEntity.procedureEntity;
        return procedureentity.id.in(subquery.select(datasetentity.procedure.id));
    }

    public BooleanExpression matchesPublicProcedure(String id) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        BooleanExpression datasetPredicate = dsFilterSpec.matchProcedures(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
