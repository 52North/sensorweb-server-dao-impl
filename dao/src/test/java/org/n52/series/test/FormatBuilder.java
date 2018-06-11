package org.n52.series.test;

import org.n52.series.db.beans.FormatEntity;

public class FormatBuilder {

    private final String format;

    private FormatBuilder(String format) {
        this.format = format;
    }

    public static FormatBuilder newFormat(String format) {
        return new FormatBuilder(format);
    }

    public FormatEntity build() {
        FormatEntity entity = new FormatEntity();
        entity.setFormat(format);
        return entity;
    }

}
