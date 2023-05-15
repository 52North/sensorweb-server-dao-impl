/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.srv;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.TagOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.series.spi.search.CategorySearchResult;
import org.n52.series.spi.search.DatasetSearchResult;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.MeasuringProgramSearchResult;
import org.n52.series.spi.search.OfferingSearchResult;
import org.n52.series.spi.search.PhenomenonSearchResult;
import org.n52.series.spi.search.PlatformSearchResult;
import org.n52.series.spi.search.ProcedureSearchResult;
import org.n52.series.spi.search.SamplingSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.SearchService;
import org.n52.series.spi.search.ServiceSearchResult;
import org.n52.series.spi.search.TagSearchResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class Search implements SearchService {

    private final OutputAssembler<ProcedureOutput> procedureRepository;

    private final OutputAssembler<PhenomenonOutput> phenomenonRepository;

    private final OutputAssembler<FeatureOutput> featureRepository;

    private final OutputAssembler<CategoryOutput> categoryRepository;

    private final OutputAssembler<PlatformOutput> platformRepository;

    private final OutputAssembler<OfferingOutput> offeringRepository;

    private final OutputAssembler<TagOutput> tagRepository;

    private final OutputAssembler<ServiceOutput> serviceRepository;

    private final OutputAssembler<DatasetOutput<?>> datasetRepository;

    private final DbQueryFactory dbQueryFactory;

    private Optional<OutputAssembler<SamplingOutput>> samplingRepository;

    private Optional<OutputAssembler<MeasuringProgramOutput>> measuringProgramRepository;

    @SuppressFBWarnings({ "EI_EXPOSE_REP2" })
    public Search(OutputAssembler<ProcedureOutput> procedureRepository,
            OutputAssembler<PhenomenonOutput> phenomenonRepository,
            OutputAssembler<FeatureOutput> featureRepository,
            OutputAssembler<CategoryOutput> categoryRepository,
            OutputAssembler<PlatformOutput> platformRepository,
            OutputAssembler<DatasetOutput<?>> datasetRepository,
            OutputAssembler<OfferingOutput> offeringRepository,
            OutputAssembler<TagOutput> tagRepository,
            OutputAssembler<ServiceOutput> serviceRepository,
            DbQueryFactory dbQueryFactory,
            Optional<OutputAssembler<SamplingOutput>> samplingRepository,
            Optional<OutputAssembler<MeasuringProgramOutput>> measuringProgramRepository) {
        this.procedureRepository = procedureRepository;
        this.phenomenonRepository = phenomenonRepository;
        this.featureRepository = featureRepository;
        this.categoryRepository = categoryRepository;
        this.platformRepository = platformRepository;
        this.datasetRepository = datasetRepository;
        this.offeringRepository = offeringRepository;
        this.tagRepository = tagRepository;
        this.serviceRepository = serviceRepository;
        this.samplingRepository = samplingRepository;
        this.measuringProgramRepository = measuringProgramRepository;
        this.dbQueryFactory = dbQueryFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SearchResult> searchResources(IoParameters parameters) {
        Set<SearchResult> results = new HashSet<>();
        Set<String> types = parameters.getSearchTypes();
        DbQuery query = dbQueryFactory.createFrom(parameters);
        if (types != null && types.isEmpty()) {
            results.addAll(phenomenonRepository.searchFor(query));
            results.addAll(procedureRepository.searchFor(query));
            results.addAll(featureRepository.searchFor(query));
            results.addAll(categoryRepository.searchFor(query));
            results.addAll(platformRepository.searchFor(query));
            results.addAll(datasetRepository.searchFor(query));
            results.addAll(offeringRepository.searchFor(query));
            results.addAll(tagRepository.searchFor(query));
            results.addAll(serviceRepository.searchFor(query));
            if (samplingRepository.isPresent()) {
                results.addAll(samplingRepository.get().searchFor(query));
            }
            if (measuringProgramRepository.isPresent()) {
                results.addAll(measuringProgramRepository.get().searchFor(query));
            }
        } else {
            if (checkType(PhenomenonSearchResult.TYPE, types)) {
                results.addAll(phenomenonRepository.searchFor(query));
            }
            if (checkType(ProcedureSearchResult.TYPE, types)) {
                results.addAll(procedureRepository.searchFor(query));
            }
            if (checkType(FeatureSearchResult.TYPE, types)) {
                results.addAll(featureRepository.searchFor(query));
            }
            if (checkType(CategorySearchResult.TYPE, types)) {
                results.addAll(categoryRepository.searchFor(query));
            }
            if (checkType(PlatformSearchResult.TYPE, types)) {
                results.addAll(platformRepository.searchFor(query));
            }
            if (checkType(DatasetSearchResult.TYPE, types)) {
                results.addAll(datasetRepository.searchFor(query));
            }
            if (checkType(OfferingSearchResult.TYPE, types)) {
                results.addAll(offeringRepository.searchFor(query));
            }
            if (checkType(TagSearchResult.TYPE, types)) {
                results.addAll(tagRepository.searchFor(query));
            }
            if (checkType(ServiceSearchResult.TYPE, types)) {
                results.addAll(serviceRepository.searchFor(query));
            }
            if (samplingRepository.isPresent() && checkType(SamplingSearchResult.TYPE, types)) {
                results.addAll(samplingRepository.get().searchFor(query));
            }
            if (measuringProgramRepository.isPresent() && checkType(MeasuringProgramSearchResult.TYPE, types)) {
                results.addAll(measuringProgramRepository.get().searchFor(query));
            }
        }
        return results;
    }

    private boolean checkType(String type, Set<String> types) {
        return types != null && types.contains(type.toLowerCase(Locale.ROOT));
    }

}
