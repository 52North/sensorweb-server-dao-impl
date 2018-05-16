
package data;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.springboot.init.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

@DataJpaTest
@RunWith(SpringRunner.class)
public class EntityManagerTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    public void foo() {
        OfferingEntity offeringEntity = new OfferingEntity();
        offeringEntity.setId(42L);
        entityManager.persist(offeringEntity);
    }
    
    @SpringBootConfiguration
    @ComponentScan(basePackageClasses = Application.class)
    static class Config {
        
    }
}
