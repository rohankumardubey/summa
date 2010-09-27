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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A {@link Configurable} is a class that has a constructor that takes
 * a {@link Configuration} as single argument. You can instantiate
 * {@link Configurable}s with {@link Configuration#create}.
 * </p><p>
 * <i>It is an error to declare a class {@code Configurable} if it
 * does not have a constructor as described above.</i>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Configurable {

    /**
     * A {@link RuntimeException} thrown by {@link Configuration#create}
     * if there is an error instantiating a {@code Configurable} via its
     * one-argument constructor that takes a {@link Configuration}.
     */
    public class ConfigurationException extends RuntimeException {
        /** Serial version UID. */
        public static final long serialVersionUID = 76846183L;

        /**
         * Creates configuration exception with a throwable cause.
         * @param cause The cause of this exception.
         */
        public ConfigurationException(final Throwable cause) {
            super(getCauseMessage(cause));
        }

        /**
         * Constructs a configuration exception with a message.
         * @param message The message.
         */
        public ConfigurationException(final String message) {
            super(message);
        }

        /**
         * Creates a configuration exception with a message and cause.
         * @param message The message.
         * @param cause The cause of this exception.
         */
        public ConfigurationException(final String message,
                                      final Throwable cause) {
            super(message, cause);
        }

        /**
         * Return the cause message.
         * @param t The throwable which this method extract the message from.
         * @return The message of the throwable.
         */
        private static String getCauseMessage(final Throwable t) {
            // If there is no message, or if the exception has its message set
            // to its class name (InvocationTargetException I am looking
            // at you!), we try and find a better message
            if (t.getMessage() == null
                || "".equals(t.getMessage())
                || t.getClass().getName().equals(t.getMessage())) {
                if (t.getCause() != null) {
                    return getCauseMessage(t.getCause());
                }
            }
            return (t.getMessage() == null || "".equals(t.getMessage())) ?
                    "Unknwon cause" : t.getMessage();
        }
    }
}
