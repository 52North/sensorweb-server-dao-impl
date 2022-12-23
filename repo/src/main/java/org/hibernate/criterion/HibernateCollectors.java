/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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
package org.hibernate.criterion;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.n52.janmayen.function.Functions;
import org.n52.janmayen.stream.Streams;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public final class HibernateCollectors {
    private HibernateCollectors() {
    }

    public static Collector<Criterion, ?, Disjunction> toDisjunction() {
        return toJunktion(Restrictions::disjunction);
    }

    public static Collector<Criterion, ?, Conjunction> toConjunction() {
        return toJunktion(Restrictions::conjunction);
    }

    public static <T extends Junction> Collector<Criterion, ?, T> toJunktion(Supplier<T> supplier) {
        BiConsumer<T, Criterion> accumulator = Junction::add;
        BinaryOperator<T> combiner = Functions.mergeLeft((d1, d2) -> Streams.stream(d2.conditions()).forEach(d1::add));
        return Collector.of(supplier, accumulator, combiner, Collector.Characteristics.UNORDERED);
    }
}
