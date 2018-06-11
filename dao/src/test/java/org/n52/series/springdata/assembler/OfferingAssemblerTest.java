package org.n52.series.springdata.assembler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.response.OfferingOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.OfferingRepository;
import org.n52.series.springdata.TestRepositories;
import org.n52.series.springdata.TestRepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingAssemblerTest {

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private DatasetRepository<DatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories<DatasetEntity> testRepositories;

    @Test
    public void foo() throws DataAccessException {
        OfferingAssembler assembler = new OfferingAssembler(offeringRepository, datasetRepository);
        DatasetEntity dataset = testRepositories.persistSimpleDataset("sml", "proc", "phen", "off", new DatasetEntity());

        DbQuery getDefaultQuery = new DefaultDbQueryFactory().createDefault();
        List<OfferingOutput> offerings = assembler.getAllCondensed(getDefaultQuery);

        Assertions.assertAll(() -> {
            assertThat(offerings).hasSize(1);
            String expected = dataset.getOffering().getIdentifier();
            assertThat(offerings.get(0).getDomainId()).isEqualTo(expected);
        });
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
