/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * An extension to HashMap<String, String> that disallows null-keys and values
 * and provides methods for converting the full map to and from Strings.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StringMap extends HashMap<String, String> {
    public static final long serialVersionUID = 868318318535L;
    public StringMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }
    public StringMap(int initialCapacity) {
        super(initialCapacity);
    }
    public StringMap() {
        super();
    }

    /**
     * Override of the constructor for HashMap that ensures that no null key
     * or values are added.
     * @param map the map to base the new map on.
     */
    public StringMap(Map<? extends String, ? extends String> map) {
        super(map.size());
        putAll(map);
    }

    /**
     * Override of HashMap.put that fails in case of null key or value.
     * @param key   the key for the value to store.
     * @param value the value to store.
     * @return the old value if any.
     */
    @Override
    public String put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        return super.put(key, value);
    }

    /**
     * Override of HashMap.putAll that fails in case of null keys or values.
     * @param map the content of the map is added to this.
     */
    @Override
    public void putAll(Map<? extends String,? extends String> map) {
        for (Map.Entry<? extends String, ? extends String> entry: map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Converts the content of the map to a single String, suitable for storage
     * in a String-only system.
     * </p><p>
     * Each entry is stored as key=value where "=" inside key or value is
     * escaped with "/e", "/" is escaped with "/s" and newline is escaped with
     * "/n". Newline is used as delimiter for entries.
     * @return a formal String-representation of the content.
     * @see #fromFormal  for the reverse function.
     */
    public String toFormal() {
        StringWriter sw = new StringWriter(size() * 50);
        boolean first = true;
        for (Map.Entry<String, String> entry: entrySet()) {
            if (!first) {
                sw.append("\n");
            }
            sw.append(escape(entry.getKey())).append("=");
            sw.append(escape(entry.getValue()));
            first = false;
        }
        return sw.toString();
    }

    /**
     * Convenience-method for packing the result of {@link #toFormal()} as a
     * utf-8 encoded byte-array.
     * @return a utf-8 encoded byte-array formal.
     */
    public byte[] toFormalBytes() {
        return toFormal().getBytes(StandardCharsets.UTF_8);
    }

    protected static String escape(String raw) {
        return raw.replace("/", "/s").replace("=", "/e").replace("\n", "/n");
    }
    protected static String unescape(String safe) {
        return safe.replace("/n", "\n").replace("/e", "=").replace("/s", "/");
    }

    /**
     * Converts the given formal String to a StringMap.
     * @param formal a formal StringMap String representation, as generated by
     *               {@link #toFormal}.
     * @return       a StringMap generated from the formal String, or null if
     *               formal is null or empty.
     */
    public static StringMap fromFormal(String formal) {
        if (formal == null || "".equals(formal)) {
            return null;
        }
        String[] lines = formal.split("\n");
        StringMap map = new StringMap(lines.length * 2);
        for (String line: lines) {
            // We don't use split here, as we want to support empty key/values
            int divPos = line.indexOf('=');
            if (divPos == -1) {
                throw new IllegalArgumentException("The line '" + line + "' did not contain an =");
            }
            map.put(unescape(line.substring(0, divPos)), unescape(line.substring(divPos+1, line.length())));
        }
        return map;
    }
    /**
     * Converts the given formal byte-array to a StringMap.
     * @param formal a formal StringMap byte-array representation, as generated
     *               by {@link #toFormalBytes}.
     * @return       a StringMap generated from the formal byte-array, or null
     *               if formal is null or empty.
     */
    public static StringMap fromFormal(byte[] formal) {
        return formal == null || formal.length == 0 ? null : fromFormal(new String(formal, StandardCharsets.UTF_8));
    }
}
