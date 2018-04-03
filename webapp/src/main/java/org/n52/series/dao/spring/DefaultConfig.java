/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package org.n52.series.dao.spring;

import org.n52.series.db.da.EntityCounter;
import org.n52.series.db.dao.DbQueryFactory;
import org.n52.series.db.dao.DefaultDbQueryFactory;
import org.n52.series.spi.srv.CountingMetadataService;
import org.n52.series.srv.CountingMetadataAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

@EnableWebMvc
@Configuration
public class DefaultConfig extends WebMvcConfigurerAdapter {

    @Value("${database.srid:'EPSG:4326'}")
    private String databaseSridCode;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Bean
    public DbQueryFactory getDefaultQueryFactory() {
        return new DefaultDbQueryFactory(databaseSridCode);
    }

    @Bean
    public EntityCounter getEntityCounter() {
        return new EntityCounter();
    }

    @Bean
    public CountingMetadataService getCountingMetadataService() {
        return new CountingMetadataAccessService();
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
                  .mediaType("json", MediaType.APPLICATION_JSON)
                  .useJaf(true);
    }

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        View jsonView = createJsonView();
        registry.enableContentNegotiation(jsonView);
    }

    private View createJsonView() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        jsonView.setExtractValueFromSingleKeyModel(true);
        jsonView.setObjectMapper(getObjectMapper());
        return jsonView;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper om = objectMapper == null
                ? createDefaultObjectMapper()
                : objectMapper;
        return om;
    }

    private ObjectMapper createDefaultObjectMapper() {
        return new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
    }

}
