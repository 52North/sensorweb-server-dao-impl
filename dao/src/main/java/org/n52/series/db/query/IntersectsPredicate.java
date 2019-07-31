/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.predicate.AbstractSimplePredicate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialRelation;
import org.locationtech.jts.geom.Geometry;

public class IntersectsPredicate extends AbstractSimplePredicate implements Expression<Boolean>, Serializable {
    private static final long serialVersionUID = -5467642277075037085L;

    private final Expression<Geometry> matchExpression;

    private final Expression<Geometry> matchingExprssion;

    private EntityManager entityManager;

    public IntersectsPredicate(CriteriaBuilderImpl criteriaBuilder, Expression<Geometry> matchExpression,
            Expression<Geometry> matchingExprssion, EntityManager entityManager) {
        super(criteriaBuilder);
        this.matchExpression = matchExpression;
        this.matchingExprssion = matchingExprssion;
        this.entityManager = entityManager;
    }

    public Expression<Geometry> getMatchExpression() {
        return matchExpression;
    }

    public Expression<Geometry> getMatchingExprssion() {
        return matchingExprssion;
    }

    @Override
    public void registerParameters(ParameterRegistry registry) {
        // Nothing to register
    }

    @Override
    public String render(boolean isNegated, RenderingContext renderingContext) {
        if (getDialect() != null && getDialect() instanceof SpatialDialect) {
            SpatialDialect dialect = (SpatialDialect) getDialect();
            StringBuilder buffer = new StringBuilder("(");
            buffer.append(dialect.getSpatialRelateSQL(((Renderable) getMatchExpression()).render(renderingContext),
                    SpatialRelation.INTERSECTS));
            buffer.append(") = TRUE");
            String spatialRelateSQL = dialect.getSpatialRelateSQL(
                    ((Renderable) getMatchExpression()).render(renderingContext), SpatialRelation.INTERSECTS);
            spatialRelateSQL = buffer.toString();
            return spatialRelateSQL.replace("?", ((Renderable) getMatchingExprssion()).render(renderingContext));
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(" ST_Intersects(").append(((Renderable) getMatchExpression()).render(renderingContext))
                .append(", ").append(((Renderable) getMatchingExprssion()).render(renderingContext))
                .append(" ) = TRUE");
        String bo = buffer.toString();

        return bo;
    }

    private Dialect getDialect() {
        if (entityManager != null) {
            Session session = (Session) entityManager.getDelegate();
            return ((SessionFactoryImplementor) session.getSessionFactory()).getServiceRegistry()
                    .getService(JdbcServices.class).getDialect();
        }
        return null;
    }
}
