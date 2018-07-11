
package org.n52.springboot.init;

import org.n52.io.extension.RenderingHintsExtension;
import org.n52.io.extension.StatusIntervalsExtension;
import org.n52.io.extension.metadata.DatabaseMetadataExtension;
import org.n52.io.extension.metadata.MetadataRepository;
import org.n52.io.extension.resulttime.ResultTimeExtension;
import org.n52.io.extension.resulttime.ResultTimeRepository;
import org.n52.io.extension.resulttime.ResultTimeService;
import org.n52.io.handler.DefaultIoFactory;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.extension.LicenseExtension;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.web.ctrl.DatasetController;
import org.n52.web.ctrl.ParameterController;
import org.n52.web.ctrl.TimeseriesMetadataController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.n52.web.ctrl")
public class ControllerConfig {

    private <T extends ParameterController<ParameterOutput>> T withLicenseExtension(T controller) {
        controller.addMetadataExtension(new LicenseExtension());
        return controller;
    }
    
    @Bean
    public DefaultIoFactory<DatasetOutput<AbstractValue< ? >>, AbstractValue< ? >> defaultIoFactory() {
        return new DefaultIoFactory<>();
    }

    @Bean
    public DatabaseMetadataExtension databaseMetadataExtension(HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        MetadataRepository repository = new MetadataRepository(sessionStore, dbQueryFactory);
        return new DatabaseMetadataExtension(repository);
    }

    @Bean
    public ResultTimeExtension resultTimeExtension(DatasetController datasetController, HibernateSessionStore sessionStore, DbQueryFactory dbQueryFactory) {
        ResultTimeRepository repository = new ResultTimeRepository(sessionStore, dbQueryFactory);
        ResultTimeService resultTimeService = new ResultTimeService(repository);
        ResultTimeExtension extension = new ResultTimeExtension(resultTimeService);
        datasetController.addMetadataExtension(extension);
        return extension;
    }

    @Bean
    public StatusIntervalsExtension<DatasetOutput< ? >> statusIntervalExtension(DatasetController datasetController) {
        StatusIntervalsExtension<DatasetOutput< ? >> extension = new StatusIntervalsExtension<>();
        datasetController.addMetadataExtension(extension);
        return extension;
    }

    @Bean
    public StatusIntervalsExtension<TimeseriesMetadataOutput> timeseriesStatusIntervalExtension(TimeseriesMetadataController timeseriesController) {
        StatusIntervalsExtension<TimeseriesMetadataOutput> extension = new StatusIntervalsExtension<>();
        timeseriesController.addMetadataExtension(extension);
        return extension;
    }

    @Bean
    public RenderingHintsExtension<DatasetOutput< ? >> renderingHintsExtension(DatasetController datasetController) {
        RenderingHintsExtension<DatasetOutput< ? >> extension = new RenderingHintsExtension<>();
        datasetController.addMetadataExtension(extension);
        return extension;
    }

    @Bean
    public RenderingHintsExtension<TimeseriesMetadataOutput> timeseriesRenderingHintsExtension(TimeseriesMetadataController timeseriesController) {
        RenderingHintsExtension<TimeseriesMetadataOutput> extension = new RenderingHintsExtension<>();
        timeseriesController.addMetadataExtension(extension);
        return extension;
    }

}
