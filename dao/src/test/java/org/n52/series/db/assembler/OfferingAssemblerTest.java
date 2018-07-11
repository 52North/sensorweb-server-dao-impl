
package org.n52.series.db.assembler;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.io.request.Parameters.HREF_BASE;
import static org.n52.io.request.Parameters.MATCH_DOMAIN_IDS;
import static org.n52.io.request.Parameters.PHENOMENA;
import static org.n52.series.test.TestUtils.getIdAsString;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.response.OfferingOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.TestBase;
import org.n52.series.db.TestRepositories;
import org.n52.series.db.TestRepositoryConfig;
import org.n52.series.db.assembler.OfferingAssembler;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingAssemblerTest extends TestBase {

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private DatasetRepository<DatasetEntity> datasetRepository;

    private OfferingAssembler assembler;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        this.assembler = new OfferingAssembler(offeringRepository, datasetRepository);
    }

    @Test
    @DisplayName("Offering of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingOfferings_then_offeringIsNotPartOfCollection() {
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Offering is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Offering id does not exist", () -> {
            final String id = getIdAsString(dataset.getOffering());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });

        Assertions.assertAll("Offering identifier does not exist", () -> {
            final DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("off", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("off", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingOfferings() {
        quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        quantityDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        quantityDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Offerings with matching domainId filters", () -> {
            final DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                                                 .replaceWith(PHENOMENA, "ph1");
            final List<OfferingOutput> offerings = assembler.getAllCondensed(ph1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .anyMatch(it -> it.equals("of1"))
                                 .anyMatch(it -> it.equals("of2"))
                                 .noneMatch(it -> it.equals("of3"));
        });
    }

    @Test
    @DisplayName("Offering output assembled properly")
    public void given_validDataset_when_queryingOffering_then_outputGetsAssembledProperly() {
        final OfferingAssembler assembler = new OfferingAssembler(offeringRepository, datasetRepository);
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        final OfferingEntity offering = dataset.getOffering();
        final String expectedId = Long.toString(offering.getId());

        final DbQuery query = defaultQuery.replaceWith(HREF_BASE, "https://foo.com/");
        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            final List<OfferingOutput> offerings = assembler.getAllCondensed(query);
            assertThat(offerings).element(0)
                                 .returns(expectedId, OfferingOutput::getId)
                                 .returns("off", OfferingOutput::getDomainId)
                                 .returns("https://foo.com/offerings/" + expectedId, OfferingOutput::getHref);

            // TODO check service, etc.
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
