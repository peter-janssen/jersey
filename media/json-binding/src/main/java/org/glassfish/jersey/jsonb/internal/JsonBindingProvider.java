/*
 * Copyright (c) 2017, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.jsonb.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import org.glassfish.jersey.jsonb.LocalizationMessages;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.glassfish.jersey.message.internal.EntityInputStream;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

/**
 * Entity provider (reader and writer) for JSONB.
 *
 * @author Adam Lindenthal
 */
@Provider
@Produces({"application/json", "text/json", "*/*"})
@Consumes({"application/json", "text/json", "*/*"})
public class JsonBindingProvider extends AbstractMessageReaderWriterProvider<Object> {

    private static final String JSON = "json";
    private static final String PLUS_JSON = "+json";

    private final Providers providers;

    @Inject
    public JsonBindingProvider(@Context Providers providers) {
        this.providers = providers;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return supportsMediaType(mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException, WebApplicationException {
        final EntityInputStream entityInputStream =  new EntityInputStream(entityStream);
        entityStream = entityInputStream;
        if (entityInputStream.isEmpty()) {
            throw new NoContentException(LocalizationMessages.ERROR_JSONB_EMPTYSTREAM());
        }

        Jsonb jsonb = getJsonb(type);

        try {
            return jsonb.fromJson(entityStream, genericType);
        } catch (JsonbException e) {
            throw new ProcessingException(LocalizationMessages.ERROR_JSONB_DESERIALIZATION(), e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return supportsMediaType(mediaType);
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        Jsonb jsonb = getJsonb(type);
        try {
            jsonb.toJson(o, genericType, new OutputStreamWriter(entityStream, getCharset(mediaType)));
        } catch (JsonbException e) {
            throw new ProcessingException(LocalizationMessages.ERROR_JSONB_SERIALIZATION(), e);
        }
    }

    private Jsonb getJsonb(Class<?> type) {
        final ContextResolver<Jsonb> contextResolver = providers.getContextResolver(Jsonb.class, MediaType.APPLICATION_JSON_TYPE);
        if (contextResolver != null) {
            return contextResolver.getContext(type);
        } else {
            return JsonbSingleton.INSTANCE.getInstance();
        }
    }

    /**
     * @return true for all media types of the pattern *&#47;json and
     * *&#47;*+json.
     */
    private static boolean supportsMediaType(final MediaType mediaType) {
        return mediaType.getSubtype().equals(JSON) || mediaType.getSubtype().endsWith(PLUS_JSON);
    }

    private enum JsonbSingleton {
        INSTANCE;

        private Jsonb jsonbInstance;

        Jsonb getInstance() {
            return jsonbInstance;
        }

        JsonbSingleton() {
            this.jsonbInstance = JsonbBuilder.create();
        }
    }
}
