
package org.n52.series.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.data.Data;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DataQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs tests against a {@link DataRepository} which has {@link DataEntity} as generic type.
 *
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class GeneralDataRepositoryTest extends TestBase {

    @Autowired
    private DataRepository<DataEntity< ? >> dataRepository;

    @Test
    @DisplayName("Quality Data are found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        final DatasetEntity dataset = quantityDataset("ph1", "of1", "pr1", "format1", "f1", "format2");
        final QuantityDataEntity dataEntity = new QuantityDataEntity();
        dataEntity.setDataset(dataset);
        dataEntity.setValue(BigDecimal.valueOf(20));
        dataEntity.setSamplingTimeStart(new Date());
        dataEntity.setSamplingTimeEnd(new Date()); // XXX why is this required?
        dataEntity.setResultTime(new Date()); // XXX why is this required?
        dataRepository.saveAndFlush(dataEntity);

        assertAll("Query quantity data", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, Data.QuantityData.VALUE_TYPE);
            final DataQuerySpecifications filterSpec = DataQuerySpecifications.of(query);
            assertThat(dataRepository.findAll()).isNotEmpty();
            final Optional<DataEntity< ? >> result = dataRepository.findOne(filterSpec.matchDatasets());
            assertThat(result).get().isInstanceOf(QuantityDataEntity.class);
        });
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories testRepositories() {
            return new TestRepositories();
        }
    }
}
