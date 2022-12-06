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
package org.n52.sensorweb.server.db.assembler;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.sensorweb.server.db.assembler.core.FormatAssembler;
import org.n52.sensorweb.server.db.assembler.mapper.OutputMapperFactory;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.ParameterDataRepository;
import org.n52.sensorweb.server.db.repositories.core.UnitRepository;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.HibernateRelations;
import org.n52.series.db.beans.UnitEntity;
import org.n52.series.db.beans.parameter.ComplexParameterEntity;
import org.n52.series.db.beans.parameter.ParameterEntity;
import org.n52.shetland.ogc.OGCConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;

import com.cosium.spring.data.jpa.entity.graph.domain.EntityGraphUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public abstract class ParameterAssembler<E extends DescribableEntity>
        implements InsertAssembler<E>, ClearAssembler<E> {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private FormatAssembler formatAssembler;

    private final ParameterDataRepository<E> parameterRepository;

    @Inject
    private UnitRepository unitRepository;

    @Lazy
    @Inject
    private OutputMapperFactory outputMapperFactory;

    public ParameterAssembler(final ParameterDataRepository<E> parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    protected abstract Specification<E> createFilterPredicate(DbQuery query);

    public E refresh(E entity) {
        entityManager.refresh(entity);
        return entity;
    }

    public Long count(DbQuery query) {
        return getParameterRepository().count(createFilterPredicate(query));
    }

    protected DatasetQuerySpecifications getDatasetQuerySpecification(DbQuery query) {
        return DatasetQuerySpecifications.of(query, entityManager);
    }

    public Stream<E> findAll(final DbQuery query) {
        return findAll(createFilterPredicate(query));
    }

    protected Stream<E> findAll(Specification<E> predicate) {
        final Iterable<E> entities =
                parameterRepository.findAll(predicate, EntityGraphUtils.fromAttributePaths("translations"));
        return StreamUtils.createStreamFromIterator(entities.iterator());
    }

    @Override
    public E checkParameter(E entity) {
        if (entity.hasParameters()) {
            Set<ParameterEntity<?>> newParams = new LinkedHashSet<>();
            for (ParameterEntity<?> parameter : entity.getParameters()) {
                checkParameter(entity, parameter, newParams);
            }
            entity.setParameters(newParams);
        }
        return entity;
    }

    public void checkParameter(E entity, ParameterEntity<?> parameter, Set<ParameterEntity<?>> newParams) {
        if (parameter instanceof ComplexParameterEntity && parameter.getValue() != null) {
            ComplexParameterEntity<?> complex = (ComplexParameterEntity<?>) parameter;
            for (Object v : (Set<?>) complex.getValue()) {
                if (v instanceof ParameterEntity) {
                    ParameterEntity<?> child = (ParameterEntity<?>) v;
                    checkParameter(entity, child, newParams);
                    child.setParent((ParameterEntity<?>) complex);
                }
            }
            complex.setValue(null);
        }
        newParams.add(parameter);
        checkUnit(parameter);
    }

    protected void checkUnit(ParameterEntity<?> parameter) {
        if (parameter instanceof HibernateRelations.HasUnit) {
            UnitEntity unit = ((HibernateRelations.HasUnit) parameter).getUnit();
            ((HibernateRelations.HasUnit) parameter).setUnit(getOrInsertUnit(unit));
        }
    }

    public UnitEntity getOrInsertUnit(UnitEntity unit) {
        if (unit != null && unit.isSetIdentifier()) {
            UnitEntity instance = unitRepository.getInstance(unit);
            if (instance != null) {
                return instance;
            }
            return unitRepository.saveAndFlush(unit);
        }
        return null;
    }

    protected FormatAssembler getFormatAssembler() {
        return formatAssembler;
    }

    protected FormatEntity getFormat(FormatEntity format) {
        return getFormatAssembler()
                .getOrInsertInstance(format != null ? format : new FormatEntity().setFormat(OGCConstants.UNKNOWN));
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public ParameterDataRepository<E> getParameterRepository() {
        return parameterRepository;
    }

    protected OutputMapperFactory getOutputMapperFactory() {
        return outputMapperFactory;
    }

}
