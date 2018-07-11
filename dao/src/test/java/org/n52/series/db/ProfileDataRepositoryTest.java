
package org.n52.series.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.n52.io.request.Parameters.FILTER_VALUE_TYPES;

import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.assertj.core.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.series.db.DataRepository;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProfileDataEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityProfileDatasetEntity;
import org.n52.series.db.beans.data.Data;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DataQuerySpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Runs tests against a {@link DataRepository} which has {@link ProfileDataEntity} as generic type.
 *
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ProfileDataRepositoryTest extends TestBase {

    @Autowired
    private DataRepository< ? super DataEntity< ? >> dataRepository;

    @Test
    @DisplayName("ProfileDataEntity data is found")
    public void given_aDatasetWithoutFeature_when_checkForAnyPublicDatasets_then_datasetIsNotFound() {
        final QuantityProfileDatasetEntity dataset = quantityProfileDataset("ph1", "of1", "pr1", "fmt1", "f1", "fmt2");
        saveProfileValues(dataset, 20.3, 42d, 0d, -1d);

        assertAll("Query quantity data", () -> {
            final DbQuery query = defaultQuery.replaceWith(FILTER_VALUE_TYPES, Data.QuantityData.VALUE_TYPE);
            final DataQuerySpecifications filterSpec = DataQuerySpecifications.of(query);
            final Iterable< ? super DataEntity< ? >> results = dataRepository.findAll(filterSpec.matchDatasets());
            assertThat(results).allMatch(it -> it instanceof QuantityDataEntity)
                               .extracting(it -> Objects.castIfBelongsToType(it, QuantityDataEntity.class))
                               .areExactly(4, new Condition<>(QuantityDataEntity::isChild, "incorrect child size"))
                               .areExactly(1, new Condition<>(QuantityDataEntity::isParent, "incorrect parent size"));
        });

        assertAll("Query profile values", () -> {

        });
    }

    private QuantityDataEntity saveProfileValues(final DatasetEntity dataset, final Double... values) {
        final Stream<Double> valueStream = values != null
            ? Stream.of(values)
            : Stream.empty();

        final Date now = new Date();
        final QuantityDataEntity profileData = new QuantityDataEntity();
        profileData.setDataset(dataset);
        profileData.setSamplingTimeStart(now);
        profileData.setSamplingTimeEnd(now);
        profileData.setResultTime(now);
        profileData.setParent(true);

        valueStream.map(it -> toQuantityData(dataset, it))
                   .forEach(dataRepository::save);
        return dataRepository.save(profileData);
    }

    private QuantityDataEntity toQuantityData(final DatasetEntity dataset, final Double it) {
        final QuantityDataEntity dataEntity = new QuantityDataEntity();
        dataEntity.setDataset(dataset);
        dataEntity.setValue(BigDecimal.valueOf(it));
        dataEntity.setSamplingTimeStart(new Date());
        dataEntity.setSamplingTimeEnd(new Date()); // XXX why is this required?
        dataEntity.setResultTime(new Date()); // XXX why is this required?
        dataEntity.setChild(true);
        return dataEntity;
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
