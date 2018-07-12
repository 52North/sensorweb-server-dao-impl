
package org.n52.series.springdata.assembler;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.io.request.Parameters.HREF_BASE;
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
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.ProcedureRepository;
import org.n52.series.springdata.TestBase;
import org.n52.series.springdata.TestRepositories;
import org.n52.series.springdata.TestRepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ProcedureAssemblerTest extends TestBase {

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private DatasetRepository<DatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories testRepositories;
    
    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    
    private ProcedureAssembler assembler;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        this.assembler = new ProcedureAssembler(procedureRepository, datasetRepository);
        // Manually start autowiring on non-spring managed Object
        beanFactory.autowireBean(assembler);
    }

    @Test
    @DisplayName("Procedure of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingProcedures_then_procedureIsNotPartOfCollection() {
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Procedure is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Procedure id does not exist", () -> {
            final String id = getIdAsString(dataset.getProcedure());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });

        Assertions.assertAll("Procedure identifier does not exist", () -> {
            final DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("proc", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("proc", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingProcedures() {
        quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        quantityDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        quantityDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Procedures with matching (by Domain ID) Phenomena filters", () -> {
            final DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                                           .replaceWith(Parameters.PHENOMENA, "ph1");
            final List<ProcedureOutput> procedures = assembler.getAllCondensed(ph1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId)
                                 .anyMatch(it -> it.equals("pr1"))
                                 .anyMatch(it -> it.equals("pr2"))
                                 .noneMatch(it -> it.equals("pr3"));
        });

        Assertions.assertAll("Procedures with matching (by Domain ID) Procedure filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.PROCEDURES, "pr1");
            
            List<ProcedureOutput> procedures = assembler.getAllCondensed(pr1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId)
                                 .anyMatch(it -> it.equals("pr1"))
                                 .noneMatch(it -> it.equals("pr2"))
                                 .noneMatch(it -> it.equals("pr3"));
        });
        

        Assertions.assertAll("Procedures with matching (by Domain ID) Feature filters", () -> {
            DbQuery fe2Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.FEATURES, "fe2");
            
            List<ProcedureOutput> procedures = assembler.getAllCondensed(fe2Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId)
                                 .noneMatch(it -> it.equals("pr1"))
                                 .anyMatch(it -> it.equals("pr2"))
                                 .anyMatch(it -> it.equals("pr3"));
        });
        
        Assertions.assertAll("Procedures with matching (by Domain ID) Offerings filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.OFFERINGS, "of1,of2");
            
            List<ProcedureOutput> procedures = assembler.getAllCondensed(pr1Query);
            assertThat(procedures).extracting(ProcedureOutput::getDomainId)
                                 .anyMatch(it -> it.equals("pr1"))
                                 .anyMatch(it -> it.equals("pr2"))
                                 .noneMatch(it -> it.equals("pr3"));
        });
        
        //TODO: Test Services Filter
        //TODO: Test Categories Filter
        //TODO: Test Platforms Filter
        //TODO: Test Stations Filter
        //TODO: Test platformTypes Filter
        //TODO: Test valueTypes Filter
    }

    @Test
    @DisplayName("Procedure output assembled properly")
    public void given_validDataset_when_queryingProcedure_then_outputGetsAssembledProperly() {
        
    	final String formatIdentifier = "TestFormat";
    	final String procedureIdentifier = "TestProcedure";
        final String procedureLabel = "TestLabel";
        
        ProcedureEntity procedure = testRepositories.upsertSimpleProcedure(procedureIdentifier, formatIdentifier);
        procedure.setIdentifier(procedureIdentifier);
        procedure.setName(procedureLabel);
        testRepositories.save(procedure);
        
        final DatasetEntity dataset = quantityDataset("phen", procedureIdentifier, "proc", "sml", "feat", formatIdentifier);
        
        final String expectedId = Long.toString(procedure.getId());
        
        final DbQuery query = defaultQuery.replaceWith(HREF_BASE, "https://foo.com/");
        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            final List<ProcedureOutput> procedures = assembler.getAllCondensed(query);
            assertThat(procedures).element(0)
                                 .returns(expectedId, ProcedureOutput::getId)
                                 .returns("proc", ProcedureOutput::getDomainId)
                                 .returns("https://foo.com/procedures/" + expectedId, ProcedureOutput::getHref);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Condensed)", () -> {
            List<ProcedureOutput> procedures = assembler.getAllCondensed(defaultQuery);
            
            ObjectAssert<ProcedureOutput> element = assertThat(procedures).element(0);
            element.extracting(ProcedureOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(ProcedureOutput::getDomainId).allMatch(it -> it.equals(procedureIdentifier));
            element.extracting(ProcedureOutput::getLabel).allMatch(it -> it.equals(procedureLabel));
            
            // Does not return unserialized fields
            element.extracting(ProcedureOutput::getExtras).allMatch(it -> it == null);
            element.extracting(ProcedureOutput::getService).allMatch(it -> it == null);
            element.extracting(ProcedureOutput::getHref).allMatch(it -> it == null);
        });
        
        Assertions.assertAll("Assert members of serialized output assemble (Expanded)", () -> {
            List<ProcedureOutput> procedures = assembler.getAllExpanded(defaultQuery);
            
            ObjectAssert<ProcedureOutput> element = assertThat(procedures).element(0);
            element.extracting(ProcedureOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(ProcedureOutput::getDomainId).allMatch(it -> it.equals(procedureIdentifier));
            element.extracting(ProcedureOutput::getLabel).allMatch(it -> it.equals(procedureLabel));
            //TODO: Check if getExtras is supposed to return null or empty collection
            element.extracting(ProcedureOutput::getExtras).allMatch(it -> it == null);
            
            element.extracting(ProcedureOutput::getService).allMatch(it -> 
            			((ServiceOutput)it).getLabel().equals("TestService") && 
						((ServiceOutput)it).getId().equals("42")
			);
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
        
        @Bean
        public ServiceEntity serviceEntity() {
            ServiceEntity serviceEntity = new ServiceEntity();
            serviceEntity.setId(42L);
            serviceEntity.setVersion("2.0");
            serviceEntity.setName("TestService");
            serviceEntity.setNoDataValues("-9999");
            return serviceEntity;
        }
    }
}
