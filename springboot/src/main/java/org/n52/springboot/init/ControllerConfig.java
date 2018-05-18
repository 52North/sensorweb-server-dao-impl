
package org.n52.springboot.init;

import org.n52.io.DefaultIoFactory;
import org.n52.io.extension.RenderingHintsExtension;
import org.n52.io.extension.StatusIntervalsExtension;
import org.n52.io.extension.metadata.DatabaseMetadataExtension;
import org.n52.io.extension.metadata.MetadataRepository;
import org.n52.io.extension.parents.HierarchicalParameterExtension;
import org.n52.io.extension.parents.HierarchicalParameterRepository;
import org.n52.io.extension.parents.HierarchicalParameterService;
import org.n52.io.extension.resulttime.ResultTimeExtension;
import org.n52.io.extension.resulttime.ResultTimeRepository;
import org.n52.io.extension.resulttime.ResultTimeService;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.GeometryInfo;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.Data;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.extension.LicenseExtension;
import org.n52.series.db.da.CategoryRepository;
import org.n52.series.db.da.DatasetRepository;
import org.n52.series.db.da.FeatureRepository;
import org.n52.series.db.da.PhenomenonRepository;
import org.n52.series.db.da.PlatformRepository;
import org.n52.series.db.da.ProcedureRepository;
import org.n52.series.db.da.StationRepository;
import org.n52.series.db.da.TimeseriesRepository;
import org.n52.series.spi.search.SearchService;
import org.n52.series.spi.srv.CountingMetadataService;
import org.n52.series.spi.srv.DataService;
import org.n52.series.spi.srv.ParameterService;
import org.n52.series.srv.Search;
import org.n52.series.srv.ServiceService;
import org.n52.web.ctrl.CategoriesParameterController;
import org.n52.web.ctrl.DataController;
import org.n52.web.ctrl.DatasetController;
import org.n52.web.ctrl.FeaturesParameterController;
import org.n52.web.ctrl.GeometriesController;
import org.n52.web.ctrl.OfferingsParameterController;
import org.n52.web.ctrl.ParameterController;
import org.n52.web.ctrl.PhenomenaParameterController;
import org.n52.web.ctrl.PlatformsParameterController;
import org.n52.web.ctrl.ProceduresParameterController;
import org.n52.web.ctrl.SearchController;
import org.n52.web.ctrl.ServicesParameterController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ControllerConfig {

    @Autowired
    private CountingMetadataService counter;

    @Bean
    public SearchController searchController(SearchService service) {
        return new SearchController(service);
    }

    @Bean
    public SearchService searchService(ProcedureRepository procedureRepository,
                                       PhenomenonRepository phenomenonRepository,
                                       FeatureRepository featureRepository,
                                       CategoryRepository categoryRepository,
                                       PlatformRepository platformRepository,
                                       DatasetRepository datasetRepository,
                                       TimeseriesRepository timeseriesRepository,
                                       StationRepository stationRepository) {
        return new Search(procedureRepository,
                          phenomenonRepository,
                          featureRepository,
                          categoryRepository,
                          platformRepository,
                          datasetRepository,
                          timeseriesRepository,
                          stationRepository);
    }

    private <T extends ParameterController<ParameterOutput>> T withLicenseExtension(T controller) {
        controller.addMetadataExtension(new LicenseExtension());
        return controller;
    }

    @Bean
    public ServicesParameterController serviceController(ServiceService service) {
        ServicesParameterController controller = new ServicesParameterController(counter, service);
        return controller;
    }

    @Bean
    public CategoriesParameterController categoryController(ParameterService<CategoryOutput> service) {
        CategoriesParameterController controller = new CategoriesParameterController(counter, service);
        return controller;
    }

    @Bean
    public FeaturesParameterController featureController(ParameterService<FeatureOutput> service) {
        FeaturesParameterController controller = new FeaturesParameterController(counter, service);
        return controller;
    }

    @Bean
    public ProceduresParameterController procedureController(ParameterService<ProcedureOutput> service) {
        ProceduresParameterController controller = new ProceduresParameterController(counter, service);
        return controller;
    }

    @Bean
    public PhenomenaParameterController phenomenonController(ParameterService<PhenomenonOutput> service) {
        PhenomenaParameterController controller = new PhenomenaParameterController(counter, service);
        return controller;
    }

    @Bean
    public OfferingsParameterController offeringController(ParameterService<OfferingOutput> service) {
        OfferingsParameterController controller = new OfferingsParameterController(counter, service);
        // XXX
        // return withLicenseExtension(controller);
        return controller;
    }

    @Bean
    public GeometriesController geometryController(ParameterService<GeometryInfo> service) {
        GeometriesController controller = new GeometriesController(counter, service);
        return controller;
    }

    @Bean
    public PlatformsParameterController platformController(ParameterService<PlatformOutput> platformService,
                                                           PlatformRepository platformRepository) {
        PlatformsParameterController controller = new PlatformsParameterController(counter, platformService);
        HierarchicalParameterRepository repository = new HierarchicalParameterRepository(platformRepository);
        HierarchicalParameterService service = new HierarchicalParameterService(repository);
        controller.addMetadataExtension(new HierarchicalParameterExtension(service));
        return controller;
    }

    // TODO refactor addition of extensions as config signature requires too
    // much knowledge when creating controller with (actually) optional extns
    // --> the config can be made much simpler when scanning for @Components

    @Bean
    public DatasetController datasetController(ParameterService<DatasetOutput< ? >> service) {
        DatasetController controller = new DatasetController(counter, service);

        controller.addMetadataExtension(renderingHintsExtension());
        controller.addMetadataExtension(statusIntervalExtension());
        controller.addMetadataExtension(resultTimeExtension());

        // XXX
        // controller.addMetadataExtension(databaseMetadataExceotion());

        return controller;
    }

    @Bean
    public DataController dataController(ParameterService<DatasetOutput<AbstractValue< ? >>> datasetService,
                                         DataService<Data<AbstractValue< ? >>> dataService) {
        return new DataController(defaultIoFactory(), datasetService, dataService);
    }

    @Bean
    public DefaultIoFactory<DatasetOutput<AbstractValue< ? >>, AbstractValue< ? >> defaultIoFactory() {
        return new DefaultIoFactory<>();
    }

    @Bean
    public DatabaseMetadataExtension databaseMetadataExtension() {
        return new DatabaseMetadataExtension(metadataRepository());
    }

    @Bean
    public MetadataRepository metadataRepository() {
        return new MetadataRepository();
    }

    @Bean
    public ResultTimeExtension resultTimeExtension() {
        return new ResultTimeExtension(resultTimeService());
    }

    @Bean
    public ResultTimeService resultTimeService() {
        return new ResultTimeService(resultTimeRepository());
    }

    @Bean
    public ResultTimeRepository resultTimeRepository() {
        return new ResultTimeRepository();
    }

    @Bean
    public StatusIntervalsExtension statusIntervalExtension() {
        return new StatusIntervalsExtension();
    }

    @Bean
    public RenderingHintsExtension renderingHintsExtension() {
        return new RenderingHintsExtension();
    }

    // @Bean
    // public TimeseriesDataController timeseriesDataController(ParameterService<QuantityDatasetOutput>
    // timeseriesMetadataService,
    // DataService<Data<QuantityValue>> timeseriesDataService) {
    // return new TimeseriesDataController(timeseriesMetadataService, timeseriesDataService);
    // }

}
