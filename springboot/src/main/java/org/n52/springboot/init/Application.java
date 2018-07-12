/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.n52.springboot.init;

import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.springdata.DatabaseConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(DatabaseConfig.class)
public class Application {
    
    @Value("${service.name:52North Dataset REST API}")
    private String name;
    
    @Value("${service.nodata.values}:''")
    private String noDataValues;
    
    
    @Bean
    public ServiceEntity serviceEntity() {
        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setId(1L);
        serviceEntity.setVersion("2.0");
        serviceEntity.setName(name);
        serviceEntity.setNoDataValues(noDataValues);
        return serviceEntity;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
