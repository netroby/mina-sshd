/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sshd.common.subsystem.sftp.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;

/**
 * @author <a href="mailto:dev@mina.apache.org">Apache MINA SSHD Project</a>
 */
public final class ParserUtils {
    public static final Collection<ExtensionParser<?>> BUILT_IN_PARSERS =
            Collections.unmodifiableList(
                    Arrays.<ExtensionParser<?>>asList(
                            VendorIdParser.INSTANCE,
                            NewlineParser.INSTANCE,
                            VersionsParser.INSTANCE
                    ));

    private static final Map<String,ExtensionParser<?>> parsersMap = new TreeMap<String,ExtensionParser<?>>(String.CASE_INSENSITIVE_ORDER) {
            private static final long serialVersionUID = 1L;    // we're not serializing it
            
            {
                for (ExtensionParser<?> p : BUILT_IN_PARSERS) {
                    put(p.getName(), p);
                }
            }
        };

    /**
     * @param parser The {@link ExtensionParser} to register
     * @return The replaced parser (by name) - {@code null} if no previous parser
     * for this extension name
     */
    public static ExtensionParser<?> registerParser(ExtensionParser<?> parser) {
        ValidateUtils.checkNotNull(parser, "No parser instance", GenericUtils.EMPTY_OBJECT_ARRAY);
        
        synchronized(parsersMap) {
            return parsersMap.put(parser.getName(), parser);
        }
    }

    /**
     * @param name The extension name - ignored if {@code null}/empty
     * @return The removed {@link ExtensionParser} - {@code null} if none registered
     * for this extension name
     */
    public static ExtensionParser<?> unregisterParser(String name) {
        if (GenericUtils.isEmpty(name)) {
            return null;
        }

        synchronized(parsersMap) {
            return parsersMap.remove(name);
        }
    }

    /**
     * @param name The extension name - ignored if {@code null}/empty
     * @return The registered {@link ExtensionParser} - {@code null} if none registered
     * for this extension name
     */
    public static ExtensionParser<?> getRegisteredParser(String name) {
        if (GenericUtils.isEmpty(name)) {
            return null;
        }

        synchronized(parsersMap) {
            return parsersMap.get(name);
        }
    }

    public static Set<String> getRegisteredParsersNames() {
        synchronized(parsersMap) {
            if (parsersMap.isEmpty()) {
                return Collections.emptySet();
            } else {    // return a copy in order to avoid concurrent modification issues
                return GenericUtils.asSortedSet(String.CASE_INSENSITIVE_ORDER, parsersMap.keySet());
            }
        }
    }

    public static final List<ExtensionParser<?>> getRegisteredParsers() {
        synchronized(parsersMap) {
            if (parsersMap.isEmpty()) {
                return Collections.emptyList();
            } else { // return a copy in order to avoid concurrent modification issues
                return new ArrayList<ExtensionParser<?>>(parsersMap.values());
            }
        }
    }

    /**
     * @param extensions The received extensions in encoded form
     * @return A {@link Map} of all the successfully decoded extensions
     * where key=extension name (same as in the original map), value=the
     * decoded extension value. Extensions for which there is no registered
     * parser are <U>ignored</U>
     * @see #getRegisteredParser(String)
     * @see ExtensionParser#transform(Object)
     */
    public static final Map<String,Object> parse(Map<String,byte[]> extensions) {
        if (GenericUtils.isEmpty(extensions)) {
            return Collections.emptyMap();
        }
        
        Map<String,Object> data = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String,byte[]> ee : extensions.entrySet()) {
            String name = ee.getKey();
            Object result = parse(name, ee.getValue());
            if (result == null) {
                continue;
            }
            data.put(name, result);
        }
        
        return data;
    }

    public static final Object parse(String name, byte ... encoded) {
        ExtensionParser<?> parser = getRegisteredParser(name);
        if (parser == null) {
            return null;
        } else {
            return parser.transform(encoded);
        }
    }
    
    private ParserUtils() {
        throw new UnsupportedOperationException("No instance");
    }
}