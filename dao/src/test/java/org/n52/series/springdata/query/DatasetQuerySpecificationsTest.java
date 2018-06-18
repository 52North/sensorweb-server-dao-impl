
package org.n52.series.springdata.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.n52.io.request.Parameters.FEATURE;
import static org.n52.io.request.Parameters.FEATURES;
import static org.n52.io.request.Parameters.OFFERING;
import static org.n52.io.request.Parameters.OFFERINGS;
import static org.n52.io.request.Parameters.PHENOMENA;
import static org.n52.io.request.Parameters.PHENOMENON;
import static org.n52.io.request.Parameters.PROCEDURE;
import static org.n52.io.request.Parameters.PROCEDURES;
import static org.n52.series.test.TestUtils.fromWkt;
import static org.n52.series.test.TestUtils.getIdAsString;
import static org.n52.series.test.TestUtils.toList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.request.Parameters;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.TestRepositories;
import org.n52.series.springdata.TestRepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.vividsolutions.jts.io.ParseException;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class DatasetQuerySpecificationsTest {

    @Autowired
    private DatasetRepository<QuantityDatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories<QuantityDatasetEntity> testRepositories;

    private DbQuery defaultQuery;

    private DatasetQuerySpecifications defaultFilterSpec;

    @BeforeEach
    public void setUp() {
        this.defaultQuery = new DefaultDbQueryFactory().createDefault();
        this.defaultFilterSpec = DatasetQuerySpecifications.of(defaultQuery);
    }

    @Test
    @DisplayName("Dataset without a feature is not found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "dataset without feature found");

    }

    @Test
    @DisplayName("Unpublished dataset is not found")
    public void given_anUnpublishedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        createSimpleQuantityDataset("ph1", "of1", "pr1", "format1").setPublished(false);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "non public dataset found");
    }

    @Test
    @DisplayName("Disabled dataset is not found")
    public void given_aDisabledDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        createSimpleQuantityDataset("ph1", "of1", "pr1", "format1").setDisabled(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "disabled dataset found");
    }

    @Test
    @DisplayName("Deleted dataset are not found")
    public void given_aDeletedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        createSimpleQuantityDataset("ph1", "of1", "pr1", "format1").setDeleted(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "deleted dataset found");
    }

    @Test
    @DisplayName("Filter datasets via offering ids")
    public void given_multipleDatasets_whenNoFilter_then_returnAllDatasets() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2");
        QuantityDatasetEntity d3 = createSimpleQuantityDataset("ph1", "of2", "pr2", "format2");
        assertThat(datasetRepository.findAll()).containsAll(toList(d1, d2, d3));
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Filter datasets via single value offering filter (backwards compatible)")
    public void given_multipleDatasets_whenQueryingWithSingleValueOfferingFilter_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getOffering());
        String id2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Single value filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(OFFERING, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Single value and list filter match in combination", () -> {
            DbQuery query = defaultQuery.replaceWith(OFFERING, id1)
                                        .replaceWith(OFFERINGS, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });
    }

    @Test
    @DisplayName("Filter datasets via offering ids")
    public void given_multipleDatasets_whenQueryingWithOfferingList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getOffering());
        String id2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Offering filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(OFFERINGS, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Offering filter matches all datasets", () -> {
            DbQuery query = defaultQuery.replaceWith(OFFERINGS, id1, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Offering filter matches no dataset", () -> {
            String notExistingId = "42";
            DbQuery query = defaultQuery.replaceWith(OFFERINGS, notExistingId);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Filter datasets via single value procedure filter (backwards compatible)")
    public void given_multipleDatasets_whenQueryingWithSingleValueProcedureFilter_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getProcedure());
        String id2 = getIdAsString(d2.getProcedure());

        Assertions.assertAll("Single value filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(PROCEDURE, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Single value and list filter match in combination", () -> {
            DbQuery query = defaultQuery.replaceWith(PROCEDURE, id1)
                                        .replaceWith(PROCEDURES, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });
    }

    @Test
    @DisplayName("Filter datasets via procedure ids")
    public void given_multipleDatasets_whenQueryingWithProcedureList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getProcedure());
        String id2 = getIdAsString(d2.getProcedure());

        Assertions.assertAll("Procedure filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(PROCEDURES, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Procedure filter matches all datasets", () -> {
            DbQuery query = defaultQuery.replaceWith(PROCEDURES, id1, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Procedure filter matches no dataset", () -> {
            String notExistingId = "42";
            DbQuery query = defaultQuery.replaceWith(PROCEDURES, notExistingId);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Filter datasets via single value phenomenon filter (backwards compatible)")
    public void given_multipleDatasets_whenQueryingWithSingleValuePhenomenonFilter_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getPhenomenon());
        String id2 = getIdAsString(d2.getPhenomenon());

        Assertions.assertAll("Single value filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(PHENOMENON, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Single value and list filter match in combination", () -> {
            DbQuery query = defaultQuery.replaceWith(PHENOMENON, id1)
                                        .replaceWith(PHENOMENA, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });
    }

    @Test
    @DisplayName("Filter datasets via phenomenon ids")
    public void given_multipleDatasets_whenQueryingWithPhenomenonList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String id1 = getIdAsString(d1.getPhenomenon());
        String id2 = getIdAsString(d2.getPhenomenon());

        Assertions.assertAll("Phenomenon filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Phenomenon filter matches all datasets", () -> {
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, id1, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Phenomenon filter matches no dataset", () -> {
            String notExistingId = "42";
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, notExistingId);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Filter datasets via single value feature filter (backwards compatible)")
    public void given_multipleDatasets_whenQueryingWithSingleValueFeatureFilter_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe2", "fe_format");

        String id1 = getIdAsString(d1.getFeature());
        String id2 = getIdAsString(d2.getFeature());

        Assertions.assertAll("Single value filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(FEATURE, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Single value and list filter match in combination", () -> {
            DbQuery query = defaultQuery.replaceWith(FEATURE, id1)
                                        .replaceWith(FEATURES, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });
    }

    @Test
    @DisplayName("Filter datasets via feature ids")
    public void given_multipleDatasets_whenQueryingWithFeatureList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format3", "fe2", "fe_format");

        String id1 = getIdAsString(d1.getFeature());
        String id2 = getIdAsString(d2.getFeature());

        Assertions.assertAll("Feature filter matches one dataset", () -> {
            DbQuery query = defaultQuery.replaceWith(FEATURES, id1);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1);
        });

        Assertions.assertAll("Feature filter matches all datasets", () -> {
            DbQuery query = defaultQuery.replaceWith(FEATURES, id1, id2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Feature filter matches no dataset", () -> {
            String notExistingId = "42";
            DbQuery query = defaultQuery.replaceWith(FEATURES, notExistingId);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }

    @Test
    @DisplayName("Filter datasets via mixed parameter ids")
    public void given_multipleDatasets_whenQueryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly() {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String offId1 = getIdAsString(d1.getOffering());
        String offId2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Mixed filter with existing parameters", () -> {
            String procId1 = getIdAsString(d1.getProcedure());
            String procId2 = getIdAsString(d2.getProcedure());
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, procId1, procId2)
                                        .replaceWith(OFFERINGS, offId1, offId2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsAll(toList(d1, d2));
        });

        Assertions.assertAll("Matching nothing at all", () -> {
            String notExistingId = "42";
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, notExistingId)
                                        .replaceWith(OFFERINGS, offId1, offId2);
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });
    }
    
    @Test
    @DisplayName("Filter datasets via bbox")
    public void given_datasetsWithGeometries_whenQueryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly() throws ParseException {
        QuantityDatasetEntity d1 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        FeatureEntity f1 = testRepositories.persistSimpleFeature("f1", "format1");
        f1.setGeometry(fromWkt("SRID=4326;POINT (7.3 52.8)"));
        d1.setFeature(f1);
        
        Assertions.assertAll("No match when laying outside bbox", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "5, 50, 6, 51");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });

        QuantityDatasetEntity d2 = createSimpleQuantityDataset("ph1", "of1", "pr1", "format1");
        FeatureEntity f2 = testRepositories.persistSimpleFeature("f2", "format1");
        f2.setGeometry(fromWkt("SRID=4326;POINT (7.2 55)"));
        d2.setFeature(f2);
        
        Assertions.assertAll("Match when laying inside bbox", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "7, 52, 7.5, 53");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).hasSize(1);
        });

    }

    private QuantityDatasetEntity createSimpleQuantityDataset(String phenomenonIdentifier,
                                                              String offeringIdentifier,
                                                              String procedureIdentifier,
                                                              String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
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
                                                     procedureIdentifier,
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
