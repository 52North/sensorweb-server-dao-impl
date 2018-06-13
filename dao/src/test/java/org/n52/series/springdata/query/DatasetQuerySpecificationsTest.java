
package org.n52.series.springdata.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.n52.series.springdata.query.DatasetQuerySpecifications.hasFeatures;
import static org.n52.series.springdata.query.DatasetQuerySpecifications.hasOfferings;
import static org.n52.series.springdata.query.DatasetQuerySpecifications.hasPhenomena;
import static org.n52.series.springdata.query.DatasetQuerySpecifications.hasProcedures;

import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.TestRepositories;
import org.n52.series.springdata.TestRepositoryConfig;
import org.n52.series.springdata.query.DatasetQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class DatasetQuerySpecificationsTest {

    @Autowired
    private DatasetRepository<QuantityDatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories<QuantityDatasetEntity> testRepositories;

    @Test
    @DisplayName("Parameters of a unpublished dataset are not found")
    public void given_anUnpublishedDataset_when_QueryingParameters_then_datasetDoesNotExist() {
        createSimpleQuantityDataset("pFormat", "procId", "phenId", "offId").setPublished(false);
        assertFalse(datasetRepository.exists(DatasetQuerySpecifications.isPublic()), "non public dataset found");
    }

    @Test
    @DisplayName("Parameters of a disabled dataset are not found")
    public void given_aDisabledDataset_when_QueryingParameters_then_datasetDoesNotExist() {
        createSimpleQuantityDataset("pFormat", "procId", "phenId", "offId").setDisabled(true);
        assertFalse(datasetRepository.exists(DatasetQuerySpecifications.isPublic()), "disabled dataset found");
    }

    @Test
    @DisplayName("Parameters of a deleted dataset are not found")
    public void given_aDeletedDataset_when_QueryingParameters_then_datasetDoesNotExist() {
        createSimpleQuantityDataset("pFormat", "procId", "phenId", "offId").setDeleted(true);
        assertFalse(datasetRepository.exists(DatasetQuerySpecifications.isPublic()), "deleted dataset found");
    }

    @Test
    @DisplayName("Filter datasets via offerings")
    public void given_multipleDatasets_whenQueryingWithOfferingList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2");

        Long offId1 = d1.getOffering().getId();
        Long offId2 = d2.getOffering().getId();
        Assertions.assertAll("Filter exclusively", () -> {
            assertIterableEquals(toList(d1), datasetRepository.findAll(hasOfferings(offId1)));
            assertIterableEquals(toList(d2), datasetRepository.findAll(hasOfferings(offId2)));
        });

        Assertions.assertAll("Matching all", () -> {
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll());
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasOfferings(offId1, offId2)));
        });

        Long notExistingOfferingId = 42L;
        Assertions.assertAll("Matching nothing at all", () -> {
            assertIterableEquals(toList(), datasetRepository.findAll(hasOfferings(notExistingOfferingId)));
        });
    }

    @Test
    @DisplayName("Filter datasets via procedures")
    public void given_multipleDatasets_whenQueryingWithProcedureList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2");

        Long procId1 = d1.getProcedure().getId();
        Long procId2 = d2.getProcedure().getId();
        Assertions.assertAll("Filter exclusively", () -> {
            assertIterableEquals(toList(d1), datasetRepository.findAll(hasProcedures(procId1)));
            assertIterableEquals(toList(d2), datasetRepository.findAll(hasProcedures(procId2)));
        });

        Assertions.assertAll("Matching all", () -> {
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll());
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasProcedures(procId1, procId2)));
        });

        Long notExistingFeatureId = 42L;
        Assertions.assertAll("Matching nothing at all", () -> {
            assertIterableEquals(toList(), datasetRepository.findAll(hasProcedures(notExistingFeatureId)));
        });
    }

    @Test
    @DisplayName("Filter datasets via phenomena")
    public void given_multipleDatasets_whenQueryingWithPhenomenonList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2");

        Long phenId1 = d1.getPhenomenon().getId();
        Long phenId2 = d2.getPhenomenon().getId();
        Assertions.assertAll("Filter exclusively", () -> {
            assertIterableEquals(toList(d1), datasetRepository.findAll(hasPhenomena(phenId1)));
            assertIterableEquals(toList(d2), datasetRepository.findAll(hasPhenomena(phenId2)));
        });

        Assertions.assertAll("Matching all", () -> {
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll());
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasPhenomena(phenId1, phenId2)));
        });

        Long notExistingFeatureId = 42L;
        Assertions.assertAll("Matching nothing at all", () -> {
            assertIterableEquals(toList(), datasetRepository.findAll(hasPhenomena(notExistingFeatureId)));
        });
    }

    @Test
    @DisplayName("Filter datasets via features")
    public void given_multipleDatasets_whenQueryingWithFeatureList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "ft1", "format2");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format3", "ft2", "format4");

        Long featId1 = d1.getFeature().getId();
        Long featId2 = d2.getFeature().getId();
        Assertions.assertAll("Filter exclusively", () -> {
            assertIterableEquals(toList(d1), datasetRepository.findAll(hasFeatures(featId1)));
            assertIterableEquals(toList(d2), datasetRepository.findAll(hasFeatures(featId2)));
        });

        Assertions.assertAll("Matching all", () -> {
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll());
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasFeatures(featId1, featId2)));
        });

        Long notExistingFeatureId = 42L;
        Assertions.assertAll("Matching nothing at all", () -> {
            assertIterableEquals(toList(), datasetRepository.findAll(hasFeatures(notExistingFeatureId)));
        });
    }

    @Test
    @DisplayName("Filter datasets via mixed parameters")
    public void given_multipleDatasets_whenQueryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2");

        Long procId1 = d1.getProcedure().getId();
        Long procId2 = d2.getProcedure().getId();
        Long phenId1 = d1.getPhenomenon().getId();
        Long phenId2 = d2.getPhenomenon().getId();
        Long offId1 = d1.getOffering().getId();
        Long offId2 = d2.getOffering().getId();

        Assertions.assertAll("Mixed filter with existing parameters", () -> {
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasProcedures(procId1, procId2).and(hasOfferings(offId1, offId2))));
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasProcedures(procId1, procId2).and(hasPhenomena(phenId1, phenId2))));
            assertIterableEquals(toList(d1, d2), datasetRepository.findAll(hasProcedures(procId1, procId2).and(hasOfferings(offId1, offId2))));
        });

        Long notExistingFeatureId = 42L;
        Assertions.assertAll("Matching nothing at all", () -> {
            assertIterableEquals(toList(), datasetRepository.findAll(hasProcedures(notExistingFeatureId).and(hasOfferings(offId2))));
            assertIterableEquals(toList(), datasetRepository.findAll(hasProcedures(notExistingFeatureId).and(hasPhenomena(phenId1, phenId2))));
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> toList(T... items) {
        return (List<T>) Arrays.asList(items);
    }

    private QuantityDatasetEntity createSimpleQuantityDataset(String phenomenonIdentifier,
                                                              String offeringIdentifier,
                                                              String procedureIdentifier,
                                                              String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     phenomenonIdentifier,
                                                     procedureFormat,
                                                     new QuantityDatasetEntity());
    }

    private QuantityDatasetEntity createSimpleQuantityDataset(String phenomenonIdentifier,
                                                              String offeringIdentifier,
                                                              String procedureIdentifier,
                                                              String procedureFormat,
                                                              String featureIdentifier,
                                                              String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     phenomenonIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new QuantityDatasetEntity());
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class)
    static class Config extends TestRepositoryConfig<QuantityDatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories<QuantityDatasetEntity> testRepositories() {
            return new TestRepositories<>();
        }
    }
}
