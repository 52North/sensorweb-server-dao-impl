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
package org.n52.series.db.old;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.loader.criteria.CriteriaJoinWalker;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.OuterJoinLoadable;

public final class DataModelUtil {

    public static boolean isPropertyNameSupported(String property, Class<?> clazz, Session session) {
        SessionFactoryImplementor factory = getSessionFactory(session);
        return hasProperty(property, factory.getClassMetadata(clazz));
    }

    public static boolean isPropertyNameSupported(String property, Criteria criteria) {
        SessionFactoryImplementor factory = extractSessionFactory(criteria);
        CriteriaImpl criteriaImpl = getCriteriaImpl(criteria);
        if (criteriaImpl == null) {
            return false;
        }
        String entityOrClassName = criteriaImpl.getEntityOrClassName();
        ClassMetadata classMetadata = factory.getClassMetadata(entityOrClassName);
        return classMetadata != null && hasProperty(property, classMetadata);
    }

    private static boolean hasProperty(String property, ClassMetadata metadata) {
        List<String> properties = Arrays.asList(metadata.getPropertyNames());
        return properties.contains(property);
    }

    public static boolean isNamedQuerySupported(String namedQuery, Session session) {
        SessionFactoryImplementor factory = getSessionFactory(session);
        NamedQueryDefinition namedQueryDef = factory.getNamedQuery(namedQuery);
        NamedSQLQueryDefinition namedSQLQueryDef =
                factory.getNamedSQLQuery(namedQuery);
        return namedQueryDef != null || namedSQLQueryDef != null;
    }

    private static SessionFactoryImplementor getSessionFactory(Session session) {
        return ((SessionImpl) session).getSessionFactory();
    }

    public static String getSqlString(Criteria criteria) {
        CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;
        SharedSessionContractImplementor session = criteriaImpl.getSession();
        SessionFactoryImplementor factory = extractSessionFactory(criteria);
        CriteriaQueryTranslator translator = new CriteriaQueryTranslator(factory,
                                                                         criteriaImpl,
                                                                         criteriaImpl.getEntityOrClassName(),
                                                                         CriteriaQueryTranslator.ROOT_SQL_ALIAS);
        String[] implementors = factory.getMetamodel().getImplementors(criteriaImpl.getEntityOrClassName());

        OuterJoinLoadable joinLoader = (OuterJoinLoadable) factory.getMetamodel().entityPersister(implementors[0]);
        CriteriaJoinWalker walker = new CriteriaJoinWalker(joinLoader,
                                                           translator,
                                                           factory,
                                                           criteriaImpl,
                                                           criteriaImpl.getEntityOrClassName(),
                                                           session.getLoadQueryInfluencers());

        return walker.getSQLString();
    }

    public static boolean isEntitySupported(Class< ? > clazz, Criteria criteria) {
        SessionFactoryImplementor factory = extractSessionFactory(criteria);

        if (factory != null) {
            return factory.getAllClassMetadata()
                          .keySet()
                          .contains(clazz.getName());
        }
        return false;
    }

    public static SessionFactoryImplementor extractSessionFactory(Criteria criteria) {
        SharedSessionContractImplementor session = getSessionImplementor(criteria);
        return session != null
                ? session.getFactory()
                : null;
    }

    private static SharedSessionContractImplementor getSessionImplementor(Criteria criteria) {
        SharedSessionContractImplementor session = null;
        if (criteria instanceof CriteriaImpl) {
            session = ((CriteriaImpl) criteria).getSession();
        } else if (criteria instanceof CriteriaImpl.Subcriteria) {
            CriteriaImpl temp = (CriteriaImpl) ((CriteriaImpl.Subcriteria) criteria).getParent();
            session = temp.getSession();
        }
        return session;
    }

    private static CriteriaImpl getCriteriaImpl(Criteria criteria) {
        if (criteria instanceof CriteriaImpl.Subcriteria) {
            return (CriteriaImpl) ((CriteriaImpl.Subcriteria) criteria).getParent();
        }
        if (criteria instanceof CriteriaImpl) {
            return (CriteriaImpl) criteria;
        }
        return null;
    }
}
