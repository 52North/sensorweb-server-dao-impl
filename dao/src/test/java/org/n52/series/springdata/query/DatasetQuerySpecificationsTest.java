
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
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.TextDatasetEntity;
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
    private DatasetRepository<DatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories<DatasetEntity> testRepositories;

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
        quantityDataset("ph1", "of1", "pr1", "format1");
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "dataset without feature found");

    }

    @Test
    @DisplayName("Unpublished dataset is not found")
    public void given_anUnpublishedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setPublished(false);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "non public dataset found");
    }

    @Test
    @DisplayName("Disabled dataset is not found")
    public void given_aDisabledDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setDisabled(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "disabled dataset found");
    }

    @Test
    @DisplayName("Deleted dataset are not found")
    public void given_aDeletedDataset_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        quantityDataset("ph1", "of1", "pr1", "format1").setDeleted(true);
        assertFalse(datasetRepository.exists(defaultFilterSpec.isPublic()), "deleted dataset found");
    }

    @Test
    @DisplayName("Filter datasets via offering ids")
    public void given_multipleDatasets_when_noFilter_then_returnAllDatasets() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2");
        DatasetEntity d3 = quantityDataset("ph1", "of2", "pr2", "format2");
        assertThat(datasetRepository.findAll()).containsAll(toList(d1, d2, d3));
    }

    @Test
    @DisplayName("Filter value types")
    public void given_textDatasets_when_queryingWithDefault_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = textDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        Assertions.assertAll("Return all by default", () -> {
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(defaultQuery);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d1, d2);
        });

        Assertions.assertAll("'all' is not a valid filter", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.FILTER_VALUE_TYPES, "all");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });

        Assertions.assertAll("Return text datasets only", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.FILTER_VALUE_TYPES, "text");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).containsOnly(d2);
        });

    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Filter datasets via single value offering filter (backwards compatible)")
    public void given_multipleDatasets_when_queryingWithSingleValueOfferingFilter_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithOfferingList_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithSingleValueProcedureFilter_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithProcedureList_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithSingleValuePhenomenonFilter_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithPhenomenonList_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithSingleValueFeatureFilter_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe2", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithFeatureList_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format3", "fe2", "fe_format");

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
    public void given_multipleDatasets_when_queryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly() {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "fe_format");
        DatasetEntity d2 = quantityDataset("ph2", "of2", "pr2", "format2", "fe1", "fe_format");

        String offId1 = getIdAsString(d1.getOffering());
        String offId2 = getIdAsString(d2.getOffering());

        Assertions.assertAll("Mixed filter with existing parameters", () -> {
            String phId1 = getIdAsString(d1.getPhenomenon());
            String phId2 = getIdAsString(d2.getPhenomenon());
            DbQuery query = defaultQuery.replaceWith(PHENOMENA, phId1, phId2)
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
    public void given_datasetsWithGeometries_when_queryingWithMixedParameterList_then_returnedDatasetsAreFilteredProperly()
            throws ParseException {
        DatasetEntity d1 = quantityDataset("ph1", "of1", "pr1", "format1");
        FeatureEntity f1 = testRepositories.persistSimpleFeature("f1", "format1");
        f1.setGeometry(fromWkt("SRID=4326;POINT (7.3 52.8)"));
        d1.setFeature(f1);

        Assertions.assertAll("No match when laying outside bbox", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "5, 50, 6, 51");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).isEmpty();
        });

        DatasetEntity d2 = quantityDataset("ph1", "of1", "pr1", "format1");
        FeatureEntity f2 = testRepositories.persistSimpleFeature("f2", "format1");
        f2.setGeometry(fromWkt("SRID=4326;POINT (7.2 55)"));
        d2.setFeature(f2);

        Assertions.assertAll("Match when laying inside bbox", () -> {
            DbQuery query = defaultQuery.replaceWith(Parameters.BBOX, "7, 52, 7.5, 53");
            DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            assertThat(datasetRepository.findAll(filterSpec.matchFilters())).hasSize(1);
        });
    }

    private DatasetEntity quantityDataset(String phenomenonIdentifier,
                                          String offeringIdentifier,
                                          String procedureIdentifier,
                                          String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new QuantityDatasetEntity());
    }

    private DatasetEntity quantityDataset(String phenomenonIdentifier,
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

    private DatasetEntity textDataset(String phenomenonIdentifier,
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
                                                     new TextDatasetEntity());
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories<DatasetEntity> testRepositories() {
            return new TestRepositories<>();
        }
    }
}
