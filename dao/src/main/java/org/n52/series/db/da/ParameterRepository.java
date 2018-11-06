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
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.web.exception.ResourceNotFoundException;

public abstract class ParameterRepository<E extends DescribableEntity, O extends ParameterOutput>
        extends SessionAwareRepository
        implements SearchableRepository, OutputAssembler<O> {

    protected abstract O prepareEmptyParameterOutput();

    protected abstract SearchResult createEmptySearchResult(String id, String label, String baseUrl);

    protected abstract AbstractDao<E> createDao(Session session);

    protected abstract SearchableDao<E> createSearchableDao(Session session);

    @Override
    public boolean exists(String id, DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return createDao(session).hasInstance(id, query);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllCondensed(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllCondensed(DbQuery query, Session session) throws DataAccessException {
        List<E> allInstances = getAllInstances(query, session);
        List<O> results = createCondensed(allInstances, query, session);
        return results;
    }

    protected List<O> createCondensed(Collection<E> allInstances, DbQuery query, Session session) {
        List<O> results = new ArrayList<>();
        for (E entity : allInstances) {
            results.add(createCondensed(entity, query, session));
        }
        return results;
    }

    protected O createCondensed(E entity, DbQuery query, Session session) {
        O result = prepareEmptyParameterOutput();
        IoParameters parameters = query.getParameters();

        Long id = entity.getId();
        String label = entity.getLabelFrom(query.getLocale());
        String domainId = entity.getIdentifier();
        String hrefBase = query.getHrefBase();

        result.setId(Long.toString(id));
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
        result.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, result::setHrefBase);
        return result;
    }

    @Override
    public List<O> getAllExpanded(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllExpanded(DbQuery query, Session session) throws DataAccessException {
        List<E> allInstances = getAllInstances(query, session);
        return createExpanded(allInstances, query, session);
    }

    protected abstract O createExpanded(E instance, DbQuery query, Session session) throws DataAccessException;

    protected List<O> createExpanded(Collection<E> allInstances, DbQuery query, Session session)
            throws DataAccessException {
        List<O> results = new ArrayList<>();
        for (E entity : allInstances) {
            O instance = createExpanded(entity, query, session);
            if (instance != null) {
                /*
                 *  there are cases where entities does not match a filter
                 *  which could not be added to a db criteria, e.g. spatial
                 *  filters on mobile platforms (last location is calculated
                 *  after db query has been finished already)
                 */
                results.add(instance);
            }
        }
        return results;
    }

    protected List<E> getAllInstances(DbQuery parameters, Session session) throws DataAccessException {
        return createDao(session).getAllInstances(parameters);
    }

    @Override
    public O getInstance(String id, DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(id, query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public O getInstance(String id, DbQuery query, Session session) throws DataAccessException {
        AbstractDao<E> dao = createDao(session);
        E entity = getEntity(parseId(id), dao, query);
        return createExpanded(entity, query, session);
    }

    protected E getInstance(Long id, DbQuery query, Session session) throws DataAccessException {
        AbstractDao<E> dao = createDao(session);
        return getEntity(id, dao, query);
    }

    protected E getEntity(Long id, AbstractDao<E> dao, DbQuery query) throws DataAccessException {
        E entity = dao.getInstance(id, query);
        if (entity == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return entity;
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        Session session = getSession();
        try {
            SearchableDao<E> dao = createSearchableDao(session);
            DbQuery query = getDbQuery(parameters);
            List<E> found = dao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    protected List<SearchResult> convertToSearchResults(List<E> found, DbQuery query) {
        String locale = query.getLocale();
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
