package org.n52.series.test;

import org.n52.series.db.beans.DescribableEntity;

public abstract class DescribableEntityBuilder<T extends DescribableEntity> {

    private final String identifier;

    public DescribableEntityBuilder(String identifier) {
        this.identifier = identifier;
    }

    protected T prepare(T entity) {
        entity.setIdentifier(identifier);
        return entity;
    }

    public abstract T build();

}
