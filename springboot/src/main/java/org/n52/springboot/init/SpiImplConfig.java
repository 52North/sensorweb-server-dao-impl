
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

package org.n52.springboot.init;

import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.GeometryInfo;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.StationOutput;
import org.n52.series.db.da.CategoryRepository;
import org.n52.series.db.da.DatasetRepository;
import org.n52.series.db.da.FeatureRepository;
import org.n52.series.db.da.GeometriesRepository;
import org.n52.series.db.da.OfferingRepository;
import org.n52.series.db.da.PhenomenonRepository;
import org.n52.series.db.da.PlatformRepository;
import org.n52.series.db.da.ProcedureRepository;
import org.n52.series.db.da.ServiceRepository;
import org.n52.series.db.da.StationRepository;
import org.n52.series.db.da.data.IDataRepositoryFactory;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.spi.srv.CountingMetadataService;
import org.n52.series.spi.srv.DataService;
import org.n52.series.spi.srv.ParameterService;
import org.n52.series.srv.AccessService;
import org.n52.series.srv.CategoryService;
import org.n52.series.srv.CountingMetadataAccessService;
import org.n52.series.srv.DatasetAccessService;
import org.n52.series.srv.FeatureService;
import org.n52.series.srv.GeometryService;
import org.n52.series.srv.OfferingService;
import org.n52.series.srv.PhenomenonService;
import org.n52.series.srv.PlatformService;
import org.n52.series.srv.ProcedureService;
import org.n52.series.srv.ServiceService;
import org.n52.series.srv.StationService;
import org.n52.web.ctrl.ParameterBackwardsCompatibilityAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = "org.n52.series.db")
public class SpiImplConfig {

    @Autowired
    private DbQueryFactory dbQueryFactory;
    
    @Bean
    public CountingMetadataService metadataService() {
        return new CountingMetadataAccessService();
    }

    @Bean
    public ParameterService<ServiceOutput> serviceParameterService(ServiceRepository repository) {
        return backwardsCompatible(serviceService(repository));
    }

    @Bean
    public ServiceService serviceService(ServiceRepository repository) {
        return new ServiceService(repository, dbQueryFactory);
    }

    @Bean
    public ParameterService<OfferingOutput> offeringParameterService(OfferingRepository repository) {
        return backwardsCompatible(new OfferingService(repository, dbQueryFactory));
    }

    @Bean
    public ParameterService<PhenomenonOutput> phenomenonParameterService(PhenomenonRepository repository) {
        return backwardsCompatible(new PhenomenonService(repository, dbQueryFactory));
    }

    @Bean
    public ParameterService<CategoryOutput> categoryParameterService(CategoryRepository repository) {
        return backwardsCompatible(new CategoryService(repository, dbQueryFactory));
    }

    @Bean
    public ParameterService<FeatureOutput> featureParameterService(FeatureRepository repository) {
        return backwardsCompatible(new FeatureService(repository, dbQueryFactory));
    }

    @Bean
    public ParameterService<ProcedureOutput> procedureParameterService(ProcedureRepository repository) {
        ProcedureService service = new ProcedureService(repository, dbQueryFactory);
        return backwardsCompatible(service);
    }

    private <T extends ParameterOutput> ParameterBackwardsCompatibilityAdapter<T> backwardsCompatible(AccessService<T> service) {
        return new ParameterBackwardsCompatibilityAdapter<>(service);
    }

    @Bean
    public ParameterService<GeometryInfo> geometryParameterService(GeometriesRepository repository) {
        return new GeometryService(repository, dbQueryFactory);
    }

    @Bean
    public ParameterService<PlatformOutput> platformParameterService(PlatformRepository repository) {
        return new PlatformService(repository, dbQueryFactory);
    }

    @Bean
    public ParameterService<StationOutput> stationParameterService(StationRepository repository) {
        return new StationService(repository, dbQueryFactory);
    }

    private DatasetAccessService createDatasetRepository(DatasetRepository<Data< ? >> repository) {
        return new DatasetAccessService(repository, dbQueryFactory);
    }

    @Bean
    public ParameterService<DatasetOutput> datasetParameterService(DatasetRepository<Data< ? >> repository) {
        return createDatasetRepository(repository);
    }

    @Bean
    public DataService<Data<AbstractValue< ? >>> dataService(DatasetRepository<Data< ? >> repository) {
        return createDatasetRepository(repository);
    }
    
}
