
package org.n52.series.springdata.assembler;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.io.request.Parameters.MATCH_DOMAIN_IDS;
import static org.n52.series.test.TestUtils.getIdAsString;

import java.util.List;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.request.Parameters;
import org.n52.io.response.OfferingOutput;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
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

    private OfferingAssembler assembler;

    private DbQuery defaultQuery;

    @BeforeEach
    public void setUp() {
        this.defaultQuery = new DefaultDbQueryFactory().createDefault();
        this.assembler = new OfferingAssembler(offeringRepository, datasetRepository);
    }

    @Test
    @DisplayName("Offering of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingOfferings_then_offeringIsNotPartOfCollection() {
        DatasetEntity dataset = createDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Offering is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Offering id does not exist", () -> {
            String id = getIdAsString(dataset.getOffering());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });
        
        Assertions.assertAll("Offering identifier does not exist", () -> {
            DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("off", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("off", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingOfferings() {
        createDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        createDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        createDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Offerings with matching domainId filters", () -> {
            DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                                           .replaceWith(Parameters.PHENOMENA, "ph1");
            List<OfferingOutput> offerings = assembler.getAllCondensed(ph1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .anyMatch(it -> it.equals("of1"))
                                 .anyMatch(it -> it.equals("of2"))
                                 .noneMatch(it -> it.equals("of3"));
        });
    }

    @Test
    @DisplayName("Offering output assembled properly")
    public void given_validDataset_when_queryingOffering_then_outputGetsAssembledProperly() {
        OfferingAssembler assembler = new OfferingAssembler(offeringRepository, datasetRepository);
        DatasetEntity dataset = createDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        OfferingEntity offering = dataset.getOffering();
        String expectedId = Long.toString(offering.getId());

        List<OfferingOutput> offerings = assembler.getAllCondensed(defaultQuery);

        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            ObjectAssert<OfferingOutput> element = assertThat(offerings).element(0);
            element.extracting(OfferingOutput::getId).anyMatch(it -> it.equals(expectedId));
            element.extracting(OfferingOutput::getDomainId).anyMatch(it -> it.equals("off"));

            // TODO check href, service, etc.
        });
    }

    private DatasetEntity createDataset(String phenomeonIdentifier,
                                        String offeringIdentifier,
                                        String procedureIdentifier,
                                        String procedureFormat,
                                        String featureIdentifier,
                                        String featureFormat) {
        return testRepositories.persistSimpleDataset(phenomeonIdentifier,
                                                     offeringIdentifier,
                                                     procedureIdentifier,
                                                     procedureFormat,
                                                     featureIdentifier,
                                                     featureFormat,
                                                     new QuantityDatasetEntity());
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
