/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.index;

import java.text.ParseException;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.w3c.dom.Node;

/**
 * Provides look-up and creation of IndexFields based on their names.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface FieldProvider<F extends IndexField> {
    /**
     * Locate and return an already existing field.
     * @param fieldName the name of the wanted field.
     * @return the field corresponding to the name.
     * @throws IllegalArgumentException if the field could not be located.
     */
    F getField(String fieldName) throws
                                                IllegalArgumentException;

    /**
     * Create an new field with default values.
     * @return a new field.
     */
    F createNewField();

    /**
     * Create a new field based on the given Document Node. The format of the
     * Node must correspond to the XML-fragment generated by F.
     * </p><p>
     * It is highly recommended to override this method to provide an
     * index-field (the obvious case is a Lucene-specific field).
     * @param node the Document Node containing the setup for the field.
     * @return a new field based on the given node.
     * @throws ParseException if the node could not be parsed.
     */
    F createNewField(Node node) throws ParseException;
}



