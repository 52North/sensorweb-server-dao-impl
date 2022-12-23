/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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
package org.n52.series.db.old.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.response.ParameterOutput;
import org.n52.sensorweb.server.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.srv.OutputAssembler;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.AbstractDao;
import org.n52.series.db.old.dao.SearchableDao;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParameterAssembler<E extends DescribableEntity, O extends ParameterOutput>
        extends SessionAwareAssembler implements SearchableAssembler, OutputAssembler<O> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterAssembler.class);

    public ParameterAssembler(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
    }

    protected abstract O prepareEmptyParameterOutput();

    protected abstract SearchResult createEmptySearchResult(String id, String label, String baseUrl);

    protected abstract AbstractDao<E> createDao(Session session);

    protected abstract SearchableDao<E> createSearchableDao(Session session);

    @Override
    public boolean exists(String id, DbQuery query) {
        Session session = getSession();
        try {
            return createDao(session).hasInstance(id, query);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllCondensed(DbQuery query) {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    private List<O> getAllCondensed(DbQuery query, Session session) {
        List<E> allInstances = getAllInstances(query, session);
        long start = System.currentTimeMillis();
        try {
            return createCondensed(allInstances, query, session);
        } finally {
            LOGGER.debug("Processing allCondensed takes: " + (System.currentTimeMillis() - start));
        }
    }

    protected List<O> createCondensed(Collection<E> allInstances, DbQuery query, Session session) {
        List<O> result = allInstances.parallelStream().map(entity -> getOutputMapper(query).createCondensed(entity))
                .filter(Objects::nonNull).collect(Collectors.toList());
        return result;
    }

    protected abstract O createCondensed(E entity, DbQuery query, Session session);

    protected abstract ParameterOutputSearchResultMapper<E, O> getOutputMapper(DbQuery query);

    protected abstract O createExpanded(E instance, DbQuery query, Session session);

    protected List<O> createExpanded(Collection<E> allInstances, DbQuery query, Session session) {
        LOGGER.debug("Entities: " + allInstances.size());
        List<O> result = allInstances.parallelStream().map(e -> getOutputMapper(query).createExpanded(e))
                .filter(Objects::nonNull).collect(Collectors.toList());
        LOGGER.debug("Ouput: " + result.size());
        return result;
    }

    @Override
    public List<O> getAllExpanded(DbQuery query) {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    private List<O> getAllExpanded(DbQuery query, Session session) {
        List<E> allInstances = getAllInstances(query, session);
        long start = System.currentTimeMillis();
        try {
            List<O> result = allInstances.parallelStream().map(entity -> getOutputMapper(query).createExpanded(entity))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            return result;
        } finally {
            LOGGER.debug("Processing allExpanded takes: " + (System.currentTimeMillis() - start));
        }
    }

    protected List<E> getAllInstances(DbQuery parameters, Session session) {
        long start = System.currentTimeMillis();
        try {
            return createDao(session).getAllInstances(parameters);
        } finally {
            LOGGER.debug("Querying allInstances takes: " + (System.currentTimeMillis() - start));
        }

    }

    @Override
    public O getInstance(String id, DbQuery query) {
        Session session = getSession();
        try {
            return getInstance(id, query, session);
        } finally {
            returnSession(session);
        }
    }

    private O getInstance(String id, DbQuery query, Session session) {
        AbstractDao<E> dao = createDao(session);
        return getEntity(parseId(id), dao, query).map(it -> getOutputMapper(query).createExpanded(it)).orElse(null);
    }

    protected E getInstance(Long id, DbQuery query, Session session) {
        AbstractDao<E> dao = createDao(session);
        return getEntity(id, dao, query).orElse(null);
    }

    protected Optional<E> getEntity(Long id, AbstractDao<E> dao, DbQuery query) {
        E entity = dao.getInstance(id, query);
        return Optional.ofNullable(entity);
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        Session session = getSession();
        try {
            SearchableDao<E> dao = createSearchableDao(session);
            List<E> found = dao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    protected List<SearchResult> convertToSearchResults(List<E> found, DbQuery query) {
        String locale = query.getLocaleForLabel();
        String hrefBase = query.getHrefBase();
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String label = searchResult.getLabelFrom(locale);
            String id = Long.toString(searchResult.getId());
            results.add(createEmptySearchResult(id, label, hrefBase));
        }
        return results;
    }

}
