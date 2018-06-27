
package org.n52.series.springdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.NotInitializedDatasetEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.beans.dataset.QuantityDataset;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.series.springdata.query.DatasetQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class DatasetRepositoryTest {

    @Autowired
    private TestRepositories<DatasetEntity> testRepositories;

    @Autowired
    private DatasetRepository<DatasetEntity> datasetRepository;

    private DbQuery defaultQuery;

    private DatasetQuerySpecifications defaultFilterSpec;

    @BeforeEach
    public void setUp() {
        this.defaultQuery = new DefaultDbQueryFactory().createDefault();
        this.defaultFilterSpec = DatasetQuerySpecifications.of(defaultQuery);
    }

    @Test
    @DisplayName("Uninitialized valueType is not found")
    public void given_uninitializedDatasetWithFeature_when_queryDatasets_then_noDatasetsFound() {
        uninitializedDataset("ph", "of", "pr", "pr_format", "f1", "format_2");
        assertAll("Uninitialized dataset returned on query", () -> {
            assertThat(datasetRepository.findAll(defaultFilterSpec.matchValueTypes())).isEmpty();
        });
    }

    @Test
    @DisplayName("Uninitialized valueType becomes findable when feature and valueType are set")
    public void given_uninitializedDataset_when_setFeatureAndQualifyAsQuantity_then_entityGetsFoundViaValueType() {
        final DatasetEntity entity = uninitializedDataset("ph", "of", "pr", "pr_format");

        entity.setFeature(testRepositories.persistSimpleFeature("f1", "format_xy"));
        datasetRepository.qualifyDataset(QuantityDataset.DATASET_TYPE, entity.getId());

        assertAll("qualified quantity dataset is found", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, QuantityDataset.DATASET_TYPE);
            final DatasetQuerySpecifications filterSpec = DatasetQuerySpecifications.of(query);
            final Optional<DatasetEntity> result = datasetRepository.findOne(filterSpec.matchValueTypes());
            assertThat(result).get().isInstanceOf(QuantityDatasetEntity.class);
        });
    }

    private DatasetEntity uninitializedDataset(final String phenomenonIdentifier,
                                               final String offeringIdentifier,
                                               final String procedureIdentifier,
                                               final String procedureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     new NotInitializedDatasetEntity());
    }

    private DatasetEntity uninitializedDataset(final String phenomenonIdentifier,
                                               final String offeringIdentifier,
                                               final String procedureIdentifier,
                                               final String procedureFormat,
                                               final String featureIdentifier,
                                               final String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomenonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new NotInitializedDatasetEntity());
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
