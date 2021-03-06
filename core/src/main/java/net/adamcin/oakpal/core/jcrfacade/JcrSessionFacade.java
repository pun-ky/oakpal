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

package net.adamcin.oakpal.core.jcrfacade;

import org.jetbrains.annotations.NotNull;

import javax.jcr.Session;

/**
 * Wraps a {@link javax.jcr.Session} to guards against writes by listeners.
 */
public final class JcrSessionFacade extends SessionFacade<Session> implements Session {

    public JcrSessionFacade(final @NotNull Session delegate, final boolean notProtected) {
        super(delegate, notProtected);
    }
}
