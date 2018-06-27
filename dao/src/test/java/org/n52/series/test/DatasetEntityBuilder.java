package org.n52.series.test;

import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;

public class DatasetEntityBuilder {

    private PhenomenonEntity phenomenon;

    private ProcedureEntity procedure;

    private OfferingEntity offering;

    private CategoryEntity category;

    private DatasetEntityBuilder() {
    }

    public <T extends DatasetEntity> T build(T entity) {
        entity.setOffering(offering);
        entity.setProcedure(procedure);
        entity.setPhenomenon(phenomenon);
        entity.setCategory(category);
        return entity;
    }

    public static DatasetEntityBuilder newDataset() {
        DatasetEntityBuilder builder = new DatasetEntityBuilder();
        return builder;
    }

    public DatasetEntityBuilder setOffering(OfferingEntity entity) {
        this.offering = entity;
        return this;
    }

    public DatasetEntityBuilder setProcedure(ProcedureEntity entity) {
        this.procedure = entity;
        return this;
    }

    public DatasetEntityBuilder setPhenomemon(PhenomenonEntity entity) {
        this.phenomenon = entity;
        return this;
    }

    public DatasetEntityBuilder setCategory(CategoryEntity entity) {
        this.category = entity;
        return this;
    }
}
