/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package integration;

import config.TestDatabaseConfig;
import integration.TestParameterRepository.TestParameterRepositoryConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.da.ProcedureRepository;
import org.n52.series.db.dao.DbQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes={TestDatabaseConfig.class, TestParameterRepositoryConfig.class})
@DataJpaTest
public class TestParameterRepository {
    
    @Configuration
    class TestParameterRepositoryConfig {
        @Bean
        public ProcedureRepository procedureRepository() {
            return new ProcedureRepository();
        }
    }

    @Autowired
    ProcedureRepository rep;

    @Autowired
    DbQueryFactory dbqueryfactory;

   @Test
   public void test() throws DataAccessException {
       System.out.println(rep.getAllExpanded(dbqueryfactory.createFrom(IoParameters.createDefaults())));
   }

}
