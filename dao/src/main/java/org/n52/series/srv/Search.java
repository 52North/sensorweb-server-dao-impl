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

package org.n52.series.srv;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.StationOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.SearchService;
import org.springframework.stereotype.Component;

@Component
public class Search implements SearchService {

    private final OutputAssembler<ProcedureOutput> procedureRepository;

    private final OutputAssembler<PhenomenonOutput> phenomenonRepository;

    private final OutputAssembler<FeatureOutput> featureRepository;

    private final OutputAssembler<CategoryOutput> categoryRepository;

    private final OutputAssembler<PlatformOutput> platformRepository;

    private final OutputAssembler<DatasetOutput< ? >> datasetRepository;

    private final OutputAssembler<TimeseriesMetadataOutput> timeseriesRepository;

    private final OutputAssembler<StationOutput> stationRepository;

    private final DbQueryFactory dbQueryFactory;

    public Search(OutputAssembler<ProcedureOutput> procedureRepository,
                  OutputAssembler<PhenomenonOutput> phenomenonRepository,
                  OutputAssembler<FeatureOutput> featureRepository,
                  OutputAssembler<CategoryOutput> categoryRepository,
                  OutputAssembler<PlatformOutput> platformRepository,
                  OutputAssembler<DatasetOutput< ? >> datasetRepository,
                  OutputAssembler<TimeseriesMetadataOutput> timeseriesRepository,
                  OutputAssembler<StationOutput> stationRepository,
                  DbQueryFactory dbQueryFactory) {
        this.procedureRepository = procedureRepository;
        this.phenomenonRepository = phenomenonRepository;
        this.featureRepository = featureRepository;
        this.categoryRepository = categoryRepository;
        this.platformRepository = platformRepository;
        this.datasetRepository = datasetRepository;
        this.timeseriesRepository = timeseriesRepository;
        this.stationRepository = stationRepository;
        this.dbQueryFactory = dbQueryFactory;
    }

    @Override
    public Collection<SearchResult> searchResources(IoParameters parameters) {
        Set<SearchResult> results = new HashSet<>();

        DbQuery query = dbQueryFactory.createFrom(parameters);
        results.addAll(phenomenonRepository.searchFor(query));
        results.addAll(procedureRepository.searchFor(query));
        results.addAll(featureRepository.searchFor(query));
        results.addAll(categoryRepository.searchFor(query));

        if (parameters.shallBehaveBackwardsCompatible()) {
            results.addAll(timeseriesRepository.searchFor(query));
            results.addAll(stationRepository.searchFor(query));
        } else {
            results.addAll(platformRepository.searchFor(query));
            results.addAll(datasetRepository.searchFor(query));
        }
        return results;
    }

}
