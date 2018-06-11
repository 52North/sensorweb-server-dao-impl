package org.n52.series.test;

import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.beans.ProcedureEntity;

public class ProcedureBuilder extends DescribableEntityBuilder<ProcedureEntity> {

    private FormatEntity format;

    private ProcedureBuilder(String identifier) {
        super(identifier);
    }

    public static ProcedureBuilder newProcedure(String identifier) {
        return new ProcedureBuilder(identifier);
    }

    @Override
    public ProcedureEntity build() {
        ProcedureEntity entity = prepare(new ProcedureEntity());
        entity.setFormat(format);
        return entity;
    }

    public ProcedureBuilder setFormat(FormatEntity format) {
        this.format = format;
        return this;
    }

}
