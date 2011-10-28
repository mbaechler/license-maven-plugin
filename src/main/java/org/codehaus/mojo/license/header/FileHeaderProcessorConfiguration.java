/*
 * #%L
 * License Maven Plugin
 * 
 * $Id: FileHeaderProcessorConfiguration.java 13519 2011-02-05 09:32:50Z tchemit $
 * $HeadURL: http://svn.codehaus.org/mojo/trunk/mojo/license-maven-plugin/src/main/java/org/codehaus/mojo/license/header/FileHeaderProcessorConfiguration.java $
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

package org.codehaus.mojo.license.header;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;

/**
 * Contract of required configuration of the {@link FileHeaderProcessor}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public interface FileHeaderProcessorConfiguration
{

    /**
     * @return mojo logger
     */
    Log getLog();

    /**
     * @return the current file header to use
     */
    FileHeader getFileHeader();

    /**
     * @return the current file transformer to use
     */
    FileHeaderTransformer getTransformer();

}
