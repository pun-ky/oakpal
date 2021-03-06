/*
 * Copyright 2018 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.oakpal.core.jcrfacade.query;

import net.adamcin.oakpal.core.jcrfacade.RangeIteratorFacade;
import net.adamcin.oakpal.core.jcrfacade.SessionFacade;
import org.jetbrains.annotations.NotNull;

import javax.jcr.Session;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Wraps {@link RowIterator} to ensure returned objects are wrapped appropriately.
 */
public final class RowIteratorFacade<S extends Session> extends RangeIteratorFacade<RowIterator> implements RowIterator {
    private final @NotNull SessionFacade<S> session;

    @SuppressWarnings("WeakerAccess")
    public RowIteratorFacade(final @NotNull RowIterator delegate, final @NotNull SessionFacade<S> session) {
        super(delegate);
        this.session = session;
    }

    @Override
    public Row nextRow() {
        Row internal = delegate.nextRow();
        return new RowFacade<>(internal, session);
    }

    @Override
    public Object next() {
        return nextRow();
    }
}
