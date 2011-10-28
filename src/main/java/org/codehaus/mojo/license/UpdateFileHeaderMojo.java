/*
 * #%L
 * License Maven Plugin
 * 
 * $Id: UpdateFileHeaderMojo.java 14741 2011-09-20 08:31:41Z tchemit $
 * $HeadURL: http://svn.codehaus.org/mojo/trunk/mojo/license-maven-plugin/src/main/java/org/codehaus/mojo/license/UpdateFileHeaderMojo.java $
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

package org.codehaus.mojo.license;

import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.license.header.*;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.model.License;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The goal to update (or add) the header on project source files.
 * <p/>
 * This goal replace the {@code update-header} goal which can not deal with
 * Copyright.
 * <p/>
 * This goal use a specific project file descriptor {@code project.xml} to
 * describe all files to update for a whole project.
 *
 * @author tchemit <chemit@codelutin.com>
 * @requiresProject true
 * @goal update-file-header
 * @since 1.0
 */
public class UpdateFileHeaderMojo
    extends AbstractLicenseNameMojo
    implements FileHeaderProcessorConfiguration
{

    /**
     * Name of project (or module).
     * <p/>
     * Will be used as description section of new header.
     *
     * @parameter expression="${license.projectName}" default-value="${project.name}"
     * @required
     * @since 1.0
     */
    protected String projectName;

    /**
     * Name of project's organization.
     * <p/>
     * Will be used as copyrigth's holder in new header.
     *
     * @parameter expression="${license.organizationName}" default-value="${project.organization.name}"
     * @required
     * @since 1.0
     */
    protected String organizationName;

    /**
     * Inception year of the project.
     * <p/>
     * Will be used as first year of copyright section in new header.
     *
     * @parameter expression="${license.inceptionYear}" default-value="${project.inceptionYear}"
     * @required
     * @since 1.0
     */
    protected String inceptionYear;

    /**
     * A flag to add svn:keywords on new header.
     * <p/>
     * Will add svn keywords :
     * <pre>Author, Id, Rev, URL and Date</pre>
     *
     * @parameter expression="${license.addSvnKeyWords}" default-value="false"
     * @since 1.0
     */
    protected boolean addSvnKeyWords;

    /**
     * A flag to authorize update of the description part of the header.
     * <p/>
     * <b>Note:</b> By default, do NOT authorize it since description can change
     * on each file).
     *
     * @parameter expression="${license.canUpdateDescription}" default-value="false"
     * @since 1.0
     */
    protected boolean canUpdateDescription;

    /**
     * A flag to authorize update of the copyright part of the header.
     * <p/>
     * <b>Note:</b> By default, do NOT authorize it since copyright part should be
     * handled by developpers (holder can change on each file for example).
     *
     * @parameter expression="${license.canUpdateCopyright}" default-value="false"
     * @since 1.0
     */
    protected boolean canUpdateCopyright;

    /**
     * A flag to authorize update of the license part of the header.
     * <p/>
     * <b>Note:</b> By default, authorize it since license part should always be
     * generated by the plugin.
     *
     * @parameter expression="${license.canUpdateLicense}" default-value="true"
     * @since 1.0
     */
    protected boolean canUpdateLicense;

    /**
     * A flag to update copyright application time (change copyright last year
     * if required) according to the last commit made on the processed file.
     * <p/>
     * Note that this functionnality is still not effective.
     *
     * @parameter expression="${license.updateCopyright}" default-value="false"
     * @since 1.0
     */
    protected boolean updateCopyright;

    /**
     * A tag to place on files that will be ignored by the plugin.
     * <p/>
     * Sometimes, it is necessary to do this when file is under a specific license.
     * <p/>
     * <b>Note:</b> If no sets, will use the default tag {@code %%Ignore-License}
     *
     * @parameter expression="${license.ignoreTag}"
     * @since 1.0
     */
    protected String ignoreTag;

    /**
     * A flag to skip the goal.
     *
     * @parameter expression="${license.skipUpdateLicense}" default-value="false"
     * @since 1.0
     */
    protected boolean skipUpdateLicense;

    /**
     * A flag to test plugin but modify no file.
     *
     * @parameter expression="${dryRun}" default-value="false"
     * @since 1.0
     */
    protected boolean dryRun;

    /**
     * A flag to clear everything after execution.
     * <p/>
     * <b>Note:</b> This property should ONLY be used for test purpose.
     *
     * @parameter expression="${license.clearAfterOperation}" default-value="true"
     * @since 1.0
     */
    protected boolean clearAfterOperation;

    /**
     * To specify the base dir from which we apply the license.
     * <p/>
     * Should be on form "root1,root2,rootn".
     * <p/>
     * By default, the main roots are "src, target/generated-sources, target/processed-sources".
     * <p/>
     * <b>Note:</b> If some of these roots do not exist, they will be simply
     * ignored.
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @parameter expression="${license.roots}"
     * @since 1.0
     */
    protected String[] roots;

    /**
     * Specific files to includes, separated by a comma. By default, it is "** /*".
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @parameter expression="${license.includes}"
     * @since 1.0
     */
    protected String[] includes;

    /**
     * Specific files to excludes, separated by a comma.
     * By default, thoses file type are excluded:
     * <ul>
     * <li>modelisation</li>
     * <li>images</li>
     * </ul>
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @parameter expression="${license.excludes}"
     * @since 1.0
     */
    protected String[] excludes;

    /**
     * To associate extra extension files to an existing comment style.
     * <p/>
     * Keys of the map are the extension of extra files to treate, and the value
     * is the comment style you want to associate.
     * <p/>
     * For example, to treate file with extensions {@code java2} and {@code jdata}
     * as {@code java} files (says using the {@code java} comment style, declare this
     * in your plugin configuration :
     * <pre>
     * &lt;extraExtensions&gt;
     * &lt;java2&gt;java&lt;/java2&gt;
     * &lt;jdata&gt;java&lt;/jdata&gt;
     * &lt;/extraExtensions&gt;
     * </pre>
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @parameter
     * @since 1.0
     */
    protected Map<String, String> extraExtensions;

    /**
     * @component role="org.nuiton.processor.Processor" roleHint="file-header"
     * @since 1.0
     */
    private FileHeaderProcessor processor;

    /**
     * The processor filter used to change header content.
     *
     * @component role="org.codehaus.mojo.license.header.FileHeaderFilter" roleHint="update-file-header"
     * @since 1.0
     */
    private UpdateFileHeaderFilter filter;

    /**
     * All available header transformers.
     *
     * @component role="org.codehaus.mojo.license.header.transformer.FileHeaderTransformer"
     * @since 1.0
     */
    private Map<String, FileHeaderTransformer> transformers;

    /**
     * internal file header transformer.
     */
    private FileHeaderTransformer transformer;

    /**
     * internal default file header.
     */
    private FileHeader header;

    /**
     * timestamp used for generation.
     */
    private long timestamp;

    /**
     * The dictionnary of extension indexed by their associated comment style.
     *
     * @since 1.0
     */
    private Map<String, String> extensionToCommentStyle;

    public static final String[] DEFAULT_INCLUDES = new String[]{ "**/*" };

    public static final String[] DEFAULT_EXCLUDES =
        new String[]{ "**/*.zargo", "**/*.uml", "**/*.umldi", "**/*.xmi", /* modelisation */
            "**/*.img", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.gif", /* images */
            "**/*.zip", "**/*.jar", "**/*.war", "**/*.ear", "**/*.tgz", "**/*.gz" };

    public static final String[] DEFAULT_ROOTS =
        new String[]{ "src", "target/generated-sources", "target/processed-sources" };

    /**
     * Defines state of a file after process.
     *
     * @author tchemit <chemit@codelutin.com>
     * @since 1.0
     */
    enum FileState
    {

        /**
         * file was updated
         */
        update,

        /**
         * file was up to date
         */
        uptodate,

        /**
         * something was added on file
         */
        add,

        /**
         * file was ignored
         */
        ignore,

        /**
         * treatment failed for file
         */
        fail;

        /**
         * register a file for this state on result dictionary.
         *
         * @param file   file to add
         * @param result dictionary to update
         */
        public void addFile( File file, EnumMap<FileState, Set<File>> result )
        {
            Set<File> fileSet = result.get( this );
            if ( fileSet == null )
            {
                fileSet = new HashSet<File>();
                result.put( this, fileSet );
            }
            fileSet.add( file );
        }
    }

    /**
     * set of processed files
     */
    private Set<File> processedFiles;

    /**
     * Dictionnary of treated files indexed by their state.
     */
    private EnumMap<FileState, Set<File>> result;

    /**
     * Dictonnary of files to treate indexed by their CommentStyle.
     */
    private Map<String, List<File>> filesToTreateByCommentStyle;

    @Override
    public void init()
        throws Exception
    {

        if ( isSkip() )
        {
            return;
        }

        if ( StringUtils.isEmpty( getIgnoreTag() ) )
        {

            // use default value
            setIgnoreTag( "%" + "%Ignore-License" );
        }

        if ( isVerbose() )
        {

            // print availables comment styles (transformers)
            StringBuilder buffer = new StringBuilder();
            buffer.append( "config - available comment styles :" );
            String commentFormat = "\n  * %1$s (%2$s)";
            for ( String transformerName : getTransformers().keySet() )
            {
                FileHeaderTransformer transformer = getTransformer( transformerName );
                String str = String.format( commentFormat, transformer.getName(), transformer.getDescription() );
                buffer.append( str );
            }
            getLog().info( buffer.toString() );
        }

        if ( isUpdateCopyright() )
        {

            getLog().warn( "\n\nupdateCopyright is not still available...\n\n" );
            //TODO-TC20100409 checks scm
            // checks scm is ok
            // for the moment, will only deal with svn except if scm
            // offers a nice api to obtain last commit date on a file

        }

        // set timestamp used for temporary files
        setTimestamp( System.nanoTime() );

        // add flags to authorize or not updates of header
        getFilter().setUpdateCopyright( isCanUpdateCopyright() );
        getFilter().setUpdateDescription( isCanUpdateDescription() );
        getFilter().setUpdateLicense( isCanUpdateLicense() );

        getFilter().setLog( getLog() );
        getProcessor().setConfiguration( this );
        getProcessor().setFilter( filter );

        super.init();

        if ( roots == null || roots.length == 0 )
        {
            roots = DEFAULT_ROOTS;
            if ( isVerbose() )
            {
                getLog().info( "Will use default roots " + Arrays.toString( roots ) );
            }
        }

        if ( includes == null || includes.length == 0 )
        {
            includes = DEFAULT_INCLUDES;
            if ( isVerbose() )
            {
                getLog().info( "Will use default includes " + Arrays.toString( includes ) );
            }
        }

        if ( excludes == null || excludes.length == 0 )
        {
            excludes = DEFAULT_EXCLUDES;
            if ( isVerbose() )
            {
                getLog().info( "Will use default excludes" + Arrays.toString( excludes ) );
            }
        }

        extensionToCommentStyle = new TreeMap<String, String>();

        // add default extensions from header transformers
        for ( Map.Entry<String, FileHeaderTransformer> entry : transformers.entrySet() )
        {
            String commentStyle = entry.getKey();
            FileHeaderTransformer transformer = entry.getValue();

            String[] extensions = transformer.getDefaultAcceptedExtensions();
            for ( String extension : extensions )
            {
                if ( isVerbose() )
                {
                    getLog().info( "Associate extension " + extension + " to comment style " + commentStyle );
                }
                extensionToCommentStyle.put( extension, commentStyle );
            }
        }

        if ( extraExtensions != null )
        {

            // fill extra extensions for each transformer
            for ( Map.Entry<String, String> entry : extraExtensions.entrySet() )
            {
                String extension = entry.getKey();
                if ( extensionToCommentStyle.containsKey( extension ) )
                {

                    // override existing extension mapping
                    getLog().warn( "The extension " + extension + " is already accepted for comment style " +
                                       extensionToCommentStyle.get( extension ) );
                }
                String commentStyle = entry.getValue();

                // check transformer exists
                getTransformer( commentStyle );

                if ( isVerbose() )
                {
                    getLog().info( "Associate extension '" + extension + "' to comment style '" + commentStyle + "'" );
                }
                extensionToCommentStyle.put( extension, commentStyle );
            }
        }

        // get all files to treate indexed by their comment style
        filesToTreateByCommentStyle = obtainFilesToTreateByCommentStyle();
    }

    protected Map<String, List<File>> obtainFilesToTreateByCommentStyle()
    {

        Map<String, List<File>> result = new HashMap<String, List<File>>();

        // add for all known comment style (says transformer) a empty list
        // this permits not to have to test if there is an already list each time
        // we wants to add a new file...
        for ( String commentStyle : transformers.keySet() )
        {
            result.put( commentStyle, new ArrayList<File>() );
        }

        List<String> rootsList = new ArrayList<String>( roots.length );
        for ( String root : roots )
        {
            File f = new File( root );
            if ( f.isAbsolute() )
            {
                rootsList.add( f.getAbsolutePath() );
            }
            else
            {
                f = new File( getProject().getBasedir(), root );
            }
            if ( f.exists() )
            {
                getLog().info( "Will search files to update from root " + f );
                rootsList.add( f.getAbsolutePath() );
            }
            else
            {
                if ( isVerbose() )
                {
                    getLog().info( "Skip not found root " + f );
                }
            }
        }

        // Obtain all files to treate
        Map<File, String[]> allFiles = new HashMap<File, String[]>();
        getFilesToTreateForRoots( includes, excludes, rootsList, allFiles );

        // filter all these files according to their extension

        for ( Map.Entry<File, String[]> entry : allFiles.entrySet() )
        {
            File root = entry.getKey();
            String[] filesPath = entry.getValue();

            // sort them by the associated comment style to their extension
            for ( String path : filesPath )
            {
                String extension = FileUtils.extension( path );
                String commentStyle = extensionToCommentStyle.get( extension );
                if ( StringUtils.isEmpty( commentStyle ) )
                {

                    // unknown extension, do not treate this file
                    continue;
                }
                //
                File file = new File( root, path );
                List<File> files = result.get( commentStyle );
                files.add( file );
            }
        }
        return result;
    }

    @Override
    public void doAction()
        throws Exception
    {

        long t0 = System.nanoTime();

        clear();

        processedFiles = new HashSet<File>();
        result = new EnumMap<FileState, Set<File>>( FileState.class );

        try
        {

            for ( Map.Entry<String, List<File>> commentStyleFiles : getFilesToTreateByCommentStyle().entrySet() )
            {

                String commentStyle = commentStyleFiles.getKey();
                List<File> files = commentStyleFiles.getValue();

                processCommentStyle( commentStyle, files );
            }

        }
        finally
        {

            int nbFiles = getProcessedFiles().size();
            if ( nbFiles == 0 )
            {
                getLog().warn( "No file to scan." );
            }
            else
            {
                String delay = MojoHelper.convertTime( System.nanoTime() - t0 );
                String message =
                    String.format( "Scan %s file%s header done in %s.", nbFiles, nbFiles > 1 ? "s" : "", delay );
                getLog().info( message );
            }
            Set<FileState> states = result.keySet();
            if ( states.size() == 1 && states.contains( FileState.uptodate ) )
            {
                // all files where up to date
                getLog().info( "All files are up-to-date." );
            }
            else
            {

                StringBuilder buffer = new StringBuilder();
                for ( FileState state : FileState.values() )
                {

                    reportType( state, buffer );
                }

                getLog().info( buffer.toString() );
            }

            // clean internal states
            if ( isClearAfterOperation() )
            {
                clear();
            }
        }
    }

    protected void processCommentStyle( String commentStyle, List<File> filesToTreat )
        throws IOException
    {

        // obtain license from definition
        License license = getLicense( getLicenseName(), true );

        getLog().info( "Process header '" + commentStyle + "'" );
        getLog().info( " - using " + license.getDescription() );

        // use header transformer according to comment style given in header
        setTransformer( getTransformer( commentStyle ) );

        // file header to use if no header is found on a file
        FileHeader defaultFileHeader =
            buildDefaultFileHeader( license, getProjectName(), getInceptionYear(), getOrganizationName(),
                                    isAddSvnKeyWords(), getEncoding() );

        // change default license header in processor
        setHeader( defaultFileHeader );

        // update processor filter
        getProcessor().populateFilter();

        for ( File file : filesToTreat )
        {
            prepareProcessFile( file );
        }
        filesToTreat.clear();
    }


    protected void prepareProcessFile( File file )
        throws IOException
    {

        if ( getProcessedFiles().contains( file ) )
        {
            getLog().info( " - skip already processed file " + file );
            return;
        }

        // output file
        File processFile = new File( file.getAbsolutePath() + "_" + getTimestamp() );
        boolean doFinalize = false;
        try
        {
            doFinalize = processFile( file, processFile );
        }
        catch ( Exception e )
        {
            getLog().warn( "skip failed file : " + e.getMessage() +
                               ( e.getCause() == null ? "" : " Cause : " + e.getCause().getMessage() ), e );
            FileState.fail.addFile( file, getResult() );
            doFinalize = false;
        }
        finally
        {

            // always clean processor internal states
            getProcessor().reset();

            // whatever was the result, this file is treated.
            getProcessedFiles().add( file );

            if ( doFinalize )
            {
                finalizeFile( file, processFile );
            }
            else
            {
                FileUtil.deleteFile( processFile );
            }
        }

    }

    /**
     * Process the given {@code file} and save the result in the given
     * {@code processFile}.
     *
     * @param file        the file to process
     * @param processFile the ouput processed file
     * @return {@code true} if prepareProcessFile can be finalize, otherwise need to be delete
     * @throws IOException if any pb while treatment
     */
    protected boolean processFile( File file, File processFile )
        throws IOException
    {

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( " - process file " + file );
            getLog().debug( " - will process into file " + processFile );
        }

        String content;

        try
        {

            // check before all that file should not be skip by the ignoreTag
            // this is a costy operation
            //TODO-TC-20100411 We should process always from the read content not reading again from file

            content = FileUtil.readAsString( file, getEncoding() );

        }
        catch ( IOException e )
        {
            throw new IOException( "Could not obtain content of file " + file );
        }

        //check that file is not marked to be ignored
        if ( content.contains( getIgnoreTag() ) )
        {
            getLog().info( " - ignore file (detected " + getIgnoreTag() + ") " + file );

            FileState.ignore.addFile( file, getResult() );

            return false;
        }

        FileHeaderProcessor processor = getProcessor();

        // process file to detect header

        try
        {
            processor.process( file, processFile );
        }
        catch ( IllegalStateException e )
        {
            // could not obtain existing header
            throw new InvalideFileHeaderException(
                "Could not extract header on file " + file + " for reason " + e.getMessage() );
        }
        catch ( Exception e )
        {
            if ( e instanceof InvalideFileHeaderException )
            {
                throw (InvalideFileHeaderException) e;
            }
            throw new IOException( "Could not process file " + file + " for reason " + e.getMessage() );
        }

        if ( processor.isTouched() )
        {

            if ( isVerbose() )
            {
                getLog().info( " - header was updated for " + file );
            }
            if ( processor.isModified() )
            {

                // header content has changed
                // must copy back process file to file (if not dry run)

                FileState.update.addFile( file, getResult() );
                return true;

            }

            FileState.uptodate.addFile( file, getResult() );
            return false;
        }

        // header was not fully (or not at all) detected in file

        if ( processor.isDetectHeader() )
        {

            // file has not a valid header (found a start process atg, but
            // not an ending one), can not do anything
            throw new InvalideFileHeaderException( "Could not find header end on file " + file );
        }

        // no header at all, add a new header

        getLog().info( " - adding license header on file " + file );

        //FIXME tchemit 20100409 xml files must add header after a xml prolog line
        content = getTransformer().addHeader( getFilter().getFullHeaderContent(), content );

        if ( !isDryRun() )
        {
            FileUtil.writeString( processFile, content, getEncoding() );
        }

        FileState.add.addFile( file, getResult() );
        return true;
    }

    protected void finalizeFile( File file, File processFile )
        throws IOException
    {

        if ( isKeepBackup() && !isDryRun() )
        {
            File backupFile = FileUtil.getBackupFile( file );

            if ( backupFile.exists() )
            {

                // always delete backup file, before the renaming
                FileUtil.deleteFile( backupFile );
            }

            if ( isVerbose() )
            {
                getLog().debug( " - backup original file " + file );
            }

            FileUtil.renameFile( file, backupFile );
        }

        if ( isDryRun() )
        {

            // dry run, delete temporary file
            FileUtil.deleteFile( processFile );
        }
        else
        {

            try
            {

                // replace file with the updated one
                FileUtil.renameFile( processFile, file );
            }
            catch ( IOException e )
            {

                // workaround windows problem to rename  files
                getLog().warn( e.getMessage() );

                // try to copy content (fail on windows xp...)
                FileUtils.copyFile( processFile, file );

                // then delete process file
                FileUtil.deleteFile( processFile );
            }
        }
    }

    @Override
    protected void finalize()
        throws Throwable
    {
        super.finalize();
        clear();
    }

    protected void clear()
    {
        Set<File> files = getProcessedFiles();
        if ( files != null )
        {
            files.clear();
        }
        EnumMap<FileState, Set<File>> result = getResult();
        if ( result != null )
        {
            for ( Set<File> fileSet : result.values() )
            {
                fileSet.clear();
            }
            result.clear();
        }
    }

    protected void reportType( FileState state, StringBuilder buffer )
    {
        String operation = state.name();

        Set<File> set = getFiles( state );
        if ( set == null || set.isEmpty() )
        {
            if ( isVerbose() )
            {
                buffer.append( "\n * no header to " );
                buffer.append( operation );
                buffer.append( "." );
            }
            return;
        }
        buffer.append( "\n * " ).append( operation ).append( " header on " );
        buffer.append( set.size() );
        if ( set.size() == 1 )
        {
            buffer.append( " file." );
        }
        else
        {
            buffer.append( " files." );
        }
        if ( isVerbose() )
        {
            for ( File file : set )
            {
                buffer.append( "\n   - " ).append( file );
            }
        }
    }

    /**
     * Build a default header given the parameters.
     *
     * @param license         the license type ot use in header
     * @param projectName     project name as header description
     * @param inceptionYear   first year of copyright
     * @param copyrightHolder holder of copyright
     * @param encoding        encoding used to read or write files
     * @param addSvnKeyWords  a flag to add in description section svn keywords
     * @return the new file header
     * @throws IOException if any problem while creating file header
     */
    protected FileHeader buildDefaultFileHeader( License license, String projectName, String inceptionYear,
                                                 String copyrightHolder, boolean addSvnKeyWords, String encoding )
        throws IOException
    {
        FileHeader result = new FileHeader();

        StringBuilder buffer = new StringBuilder();
        buffer.append( projectName );
        if ( addSvnKeyWords )
        {
            // add svn keyworks
            char ls = FileHeaderTransformer.LINE_SEPARATOR;
            buffer.append( ls );

            // breaks the keyword otherwise svn will update them here
            //TC-20100415 : do not generate thoses redundant keywords
//            buffer.append(ls).append("$" + "Author$");
//            buffer.append(ls).append("$" + "LastChangedDate$");
//            buffer.append(ls).append("$" + "LastChangedRevision$");
            buffer.append( ls ).append( "$" + "Id$" );
            buffer.append( ls ).append( "$" + "HeadURL$" );

        }
        result.setDescription( buffer.toString() );
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "header description : " + result.getDescription() );
        }

        String licenseContent = license.getHeaderContent( encoding );
        result.setLicense( licenseContent );

        Integer firstYear = Integer.valueOf( inceptionYear );
        result.setCopyrightFirstYear( firstYear );

        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date() );
        Integer lastYear = cal.get( Calendar.YEAR );
        if ( firstYear < lastYear )
        {
            result.setCopyrightLastYear( lastYear );
        }
        result.setCopyrightHolder( copyrightHolder );
        return result;
    }

    public FileHeaderTransformer getTransformer( String transformerName )
        throws IllegalArgumentException, IllegalStateException
    {
        if ( StringUtils.isEmpty( transformerName ) )
        {
            throw new IllegalArgumentException( "transformerName can not be null, nor empty!" );
        }
        Map<String, FileHeaderTransformer> transformers = getTransformers();
        if ( transformers == null )
        {
            throw new IllegalStateException( "No transformers initialized!" );
        }
        FileHeaderTransformer transformer = transformers.get( transformerName );
        if ( transformer == null )
        {
            throw new IllegalArgumentException(
                "transformerName " + transformerName + " is unknow, use one this one : " + transformers.keySet() );
        }
        return transformer;
    }

    public boolean isClearAfterOperation()
    {
        return clearAfterOperation;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public String getInceptionYear()
    {
        return inceptionYear;
    }

    public String getOrganizationName()
    {
        return organizationName;
    }

    public boolean isUpdateCopyright()
    {
        return updateCopyright;
    }

    public boolean isCanUpdateDescription()
    {
        return canUpdateDescription;
    }

    public boolean isCanUpdateCopyright()
    {
        return canUpdateCopyright;
    }

    public boolean isCanUpdateLicense()
    {
        return canUpdateLicense;
    }

    public String getIgnoreTag()
    {
        return ignoreTag;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public UpdateFileHeaderFilter getFilter()
    {
        return filter;
    }

    public FileHeader getFileHeader()
    {
        return header;
    }

    public FileHeaderTransformer getTransformer()
    {
        return transformer;
    }

    @Override
    public boolean isSkip()
    {
        return skipUpdateLicense;
    }

    public Set<File> getProcessedFiles()
    {
        return processedFiles;
    }

    public EnumMap<FileState, Set<File>> getResult()
    {
        return result;
    }

    public Set<File> getFiles( FileState state )
    {
        return result.get( state );
    }

    public boolean isAddSvnKeyWords()
    {
        return addSvnKeyWords;
    }

    public FileHeaderProcessor getProcessor()
    {
        return processor;
    }

    public Map<String, FileHeaderTransformer> getTransformers()
    {
        return transformers;
    }

    public Map<String, List<File>> getFilesToTreateByCommentStyle()
    {
        return filesToTreateByCommentStyle;
    }

    @Override
    public void setSkip( boolean skipUpdateLicense )
    {
        this.skipUpdateLicense = skipUpdateLicense;
    }

    public void setDryRun( boolean dryRun )
    {
        this.dryRun = dryRun;
    }

    public void setTimestamp( long timestamp )
    {
        this.timestamp = timestamp;
    }

    public void setProjectName( String projectName )
    {
        this.projectName = projectName;
    }

    public void setSkipUpdateLicense( boolean skipUpdateLicense )
    {
        this.skipUpdateLicense = skipUpdateLicense;
    }

    public void setInceptionYear( String inceptionYear )
    {
        this.inceptionYear = inceptionYear;
    }

    public void setOrganizationName( String organizationName )
    {
        this.organizationName = organizationName;
    }

    public void setUpdateCopyright( boolean updateCopyright )
    {
        this.updateCopyright = updateCopyright;
    }

    public void setIgnoreTag( String ignoreTag )
    {
        this.ignoreTag = ignoreTag;
    }

    public void setAddSvnKeyWords( boolean addSvnKeyWords )
    {
        this.addSvnKeyWords = addSvnKeyWords;
    }

    public void setClearAfterOperation( boolean clearAfterOperation )
    {
        this.clearAfterOperation = clearAfterOperation;
    }

    public void setTransformer( FileHeaderTransformer transformer )
    {
        this.transformer = transformer;
    }

    public void setHeader( FileHeader header )
    {
        this.header = header;
    }

    public void setProcessor( FileHeaderProcessor processor )
    {
        this.processor = processor;
    }

    public void setFilter( UpdateFileHeaderFilter filter )
    {
        this.filter = filter;
    }

    public void setCanUpdateDescription( boolean canUpdateDescription )
    {
        this.canUpdateDescription = canUpdateDescription;
    }

    public void setCanUpdateCopyright( boolean canUpdateCopyright )
    {
        this.canUpdateCopyright = canUpdateCopyright;
    }

    public void setCanUpdateLicense( boolean canUpdateLicense )
    {
        this.canUpdateLicense = canUpdateLicense;
    }

    public void setTransformers( Map<String, FileHeaderTransformer> transformers )
    {
        this.transformers = transformers;
    }

    public void setFilesToTreateByCommentStyle( Map<String, List<File>> filesToTreateByCommentStyle )
    {
        this.filesToTreateByCommentStyle = filesToTreateByCommentStyle;
    }

    public void setRoots( String[] roots )
    {
        this.roots = roots;
    }

    public void setRoots( String roots )
    {
        this.roots = roots.split( "," );
    }

    public void setIncludes( String[] includes )
    {
        this.includes = includes;
    }

    public void setIncludes( String includes )
    {
        this.includes = includes.split( "," );
    }

    public void setExcludes( String[] excludes )
    {
        this.excludes = excludes;
    }

    public void setExcludes( String excludes )
    {
        this.excludes = excludes.split( "," );
    }

    /**
     * Collects some file.
     *
     * @param includes includes
     * @param excludes excludes
     * @param roots    root directories to treate
     * @param files    cache of file detected indexed by their root directory
     */
    protected void getFilesToTreateForRoots( String[] includes, String[] excludes, List<String> roots,
                                             Map<File, String[]> files )
    {

        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes( includes );
        if ( excludes != null )
        {
            ds.setExcludes( excludes );
        }
        for ( String src : roots )
        {

            File f = new File( src );
            if ( !f.exists() )
            {
                // do nothing on a non-existent
                continue;
            }

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "discovering source files in " + src );
            }

            ds.setBasedir( f );
            // scan
            ds.scan();

            // get files
            String[] tmp = ds.getIncludedFiles();

            if ( tmp.length < 1 )
            {
                // no files found
                continue;
            }

            List<String> toTreate = new ArrayList<String>();

            for ( String filePath : tmp )
            {
                File srcFile = new File( f, filePath );
                // check file is up-to-date
                toTreate.add( filePath );
            }

            if ( toTreate.isEmpty() )
            {
                // no file or all are up-to-date
                continue;
            }

            // register files
            files.put( f, toTreate.toArray( new String[toTreate.size()] ) );
        }
    }
}