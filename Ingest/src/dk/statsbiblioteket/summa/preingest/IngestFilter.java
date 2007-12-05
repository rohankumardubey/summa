/* $Id: IngestFilter.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.preingest;

import java.io.File;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * An filter in the Summa pre-ingest needs to implement this interface and to have an default empty constructor.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public interface IngestFilter {

    /**
     * Filters the input file, the filter process will create a new File adding the given
     * Extension to the input.<br>
     *
     * @param input             the file to filter.
     * @param ext               the extension on the filtered data.
     * @param encoding          the encoding used in the input.
     */
    public void applyFilter(File input, Extension ext, String encoding);


}
