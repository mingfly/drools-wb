/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.dtable.backend.server;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Charsets;
import org.drools.workbench.models.datamodel.oracle.PackageDataModelOracle;
import org.drools.workbench.models.datamodel.workitems.PortableWorkDefinition;
import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.DTCellValue52;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.model.GuidedDecisionTableEditorContent;
import org.drools.workbench.screens.guided.dtable.service.GuidedDecisionTableEditorService;
import org.drools.workbench.screens.guided.dtable.type.GuidedDTableResourceTypeDefinition;
import org.drools.workbench.screens.workitems.service.WorkItemsEditorService;
import org.guvnor.common.services.backend.config.SafeSessionInfo;
import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.backend.file.FileExtensionFilter;
import org.guvnor.common.services.backend.file.JavaFileFilter;
import org.guvnor.common.services.backend.util.CommentedOptionFactory;
import org.guvnor.common.services.backend.validation.GenericValidator;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.metadata.model.Overview;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.workbench.common.services.backend.file.DRLFileFilter;
import org.kie.workbench.common.services.backend.file.DSLFileFilter;
import org.kie.workbench.common.services.backend.file.DSLRFileFilter;
import org.kie.workbench.common.services.backend.file.GlobalsFileFilter;
import org.kie.workbench.common.services.backend.file.RDRLFileFilter;
import org.kie.workbench.common.services.backend.file.RDSLRFileFilter;
import org.kie.workbench.common.services.backend.service.KieService;
import org.kie.workbench.common.services.datamodel.backend.server.DataModelOracleUtilities;
import org.kie.workbench.common.services.datamodel.backend.server.service.DataModelService;
import org.kie.workbench.common.services.datamodel.model.PackageDataModelOracleBaselinePayload;
import org.kie.workbench.common.services.shared.project.KieProject;
import org.kie.workbench.common.services.shared.project.KieProjectService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.service.CopyService;
import org.uberfire.ext.editor.commons.service.DeleteService;
import org.uberfire.ext.editor.commons.service.RenameService;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.DirectoryStream;
import org.uberfire.java.nio.file.FileAlreadyExistsException;
import org.uberfire.java.nio.file.Files;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.ResourceOpenedEvent;

@Service
@ApplicationScoped
public class GuidedDecisionTableEditorServiceImpl
        extends KieService<GuidedDecisionTableEditorContent>
        implements GuidedDecisionTableEditorService {

    //Filters to include *all* applicable resources
    private static final JavaFileFilter FILTER_JAVA = new JavaFileFilter();
    private static final DRLFileFilter FILTER_DRL = new DRLFileFilter();
    private static final DSLRFileFilter FILTER_DSLR = new DSLRFileFilter();
    private static final DSLFileFilter FILTER_DSL = new DSLFileFilter();
    private static final RDRLFileFilter FILTER_RDRL = new RDRLFileFilter();
    private static final RDSLRFileFilter FILTER_RDSLR = new RDSLRFileFilter();
    private static final GlobalsFileFilter FILTER_GLOBAL = new GlobalsFileFilter();
    private static final FileExtensionFilter FILTER_GUIDED_DECISION_TABLES = new FileExtensionFilter( ".gdst" );

    private IOService ioService;
    private CopyService copyService;
    private DeleteService deleteService;
    private RenameService renameService;
    private DataModelService dataModelService;
    private WorkItemsEditorService workItemsService;
    private KieProjectService projectService;
    private Event<ResourceOpenedEvent> resourceOpenedEvent;
    private GenericValidator genericValidator;
    private CommentedOptionFactory commentedOptionFactory;
    private GuidedDTableResourceTypeDefinition resourceType;
    private SafeSessionInfo safeSessionInfo;

    public GuidedDecisionTableEditorServiceImpl() {
        //Zero parameter constructor for CDI
    }

    @Inject
    public GuidedDecisionTableEditorServiceImpl( final @Named("ioStrategy") IOService ioService,
                                                 final CopyService copyService,
                                                 final DeleteService deleteService,
                                                 final RenameService renameService,
                                                 final DataModelService dataModelService,
                                                 final WorkItemsEditorService workItemsService,
                                                 final KieProjectService projectService,
                                                 final Event<ResourceOpenedEvent> resourceOpenedEvent,
                                                 final GenericValidator genericValidator,
                                                 final CommentedOptionFactory commentedOptionFactory,
                                                 final GuidedDTableResourceTypeDefinition resourceType,
                                                 final SessionInfo sessionInfo ) {
        this.ioService = ioService;
        this.copyService = copyService;
        this.deleteService = deleteService;
        this.renameService = renameService;
        this.dataModelService = dataModelService;
        this.workItemsService = workItemsService;
        this.projectService = projectService;
        this.resourceOpenedEvent = resourceOpenedEvent;
        this.genericValidator = genericValidator;
        this.commentedOptionFactory = commentedOptionFactory;
        this.resourceType = resourceType;
        this.safeSessionInfo = new SafeSessionInfo( sessionInfo );
    }

    @Override
    public Path create( final Path context,
                        final String fileName,
                        final GuidedDecisionTable52 content,
                        final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( context );
            final String packageName = ( pkg == null ? null : pkg.getPackageName() );
            content.setPackageName( packageName );

            final org.uberfire.java.nio.file.Path nioPath = Paths.convert( context ).resolve( fileName );
            final Path newPath = Paths.convert( nioPath );

            if ( ioService.exists( nioPath ) ) {
                throw new FileAlreadyExistsException( nioPath.toString() );
            }

            ioService.write( nioPath,
                             GuidedDTXMLPersistence.getInstance().marshal( content ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            return newPath;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public GuidedDecisionTable52 load( final Path path ) {
        try {
            final String content = ioService.readAllString( Paths.convert( path ) );

            return GuidedDTXMLPersistence.getInstance().unmarshal( content );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public GuidedDecisionTableEditorContent loadContent( final Path path ) {
        return super.loadContent( path );
    }

    @Override
    protected GuidedDecisionTableEditorContent constructContent( Path path,
                                                                 Overview overview ) {
        final GuidedDecisionTable52 model = load( path );
        final PackageDataModelOracle oracle = dataModelService.getDataModel( path );
        final PackageDataModelOracleBaselinePayload dataModel = new PackageDataModelOracleBaselinePayload();

        //Get FQCN's used by model
        final GuidedDecisionTableModelVisitor visitor = new GuidedDecisionTableModelVisitor( model );
        final Set<String> consumedFQCNs = visitor.getConsumedModelClasses();

        //Get FQCN's used by Globals
        consumedFQCNs.addAll( oracle.getPackageGlobals().values() );

        DataModelOracleUtilities.populateDataModel( oracle,
                                                    dataModel,
                                                    consumedFQCNs );

        final Set<PortableWorkDefinition> workItemDefinitions = workItemsService.loadWorkItemDefinitions( path );

        //Signal opening to interested parties
        resourceOpenedEvent.fire( new ResourceOpenedEvent( path,
                                                           safeSessionInfo ) );

        return new GuidedDecisionTableEditorContent( model,
                                                     workItemDefinitions,
                                                     overview,
                                                     dataModel );
    }

    @Override
    public PackageDataModelOracleBaselinePayload loadDataModel( final Path path ) {
        try {
            final PackageDataModelOracle oracle = dataModelService.getDataModel( path );
            final PackageDataModelOracleBaselinePayload dataModel = new PackageDataModelOracleBaselinePayload();
            //There are no classes to pre-load into the DMO when requesting a new Data Model only
            DataModelOracleUtilities.populateDataModel( oracle,
                                                        dataModel,
                                                        new HashSet<String>() );

            return dataModel;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path save( final Path resource,
                      final GuidedDecisionTable52 model,
                      final Metadata metadata,
                      final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( resource );
            final String packageName = ( pkg == null ? null : pkg.getPackageName() );
            model.setPackageName( packageName );

            Metadata currentMetadata = metadataService.getMetadata( resource );
            ioService.write( Paths.convert( resource ),
                             GuidedDTXMLPersistence.getInstance().marshal( model ),
                             metadataService.setUpAttributes( resource,
                                                              metadata ),
                             commentedOptionFactory.makeCommentedOption( comment ) );

            fireMetadataSocialEvents( resource, currentMetadata, metadata );
            return resource;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public void delete( final Path path,
                        final String comment ) {
        try {
            deleteService.delete( path,
                                  comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path rename( final Path path,
                        final String newName,
                        final String comment ) {
        try {
            return renameService.rename( path,
                                         newName,
                                         comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path copy( final Path path,
                      final String newName,
                      final String comment ) {
        try {
            return copyService.copy( path,
                                     newName,
                                     comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path copy( final Path path,
                      final String newName,
                      final Path targetDirectory,
                      final String comment ) {
        try {
            return copyService.copy( path,
                                     newName,
                                     targetDirectory,
                                     comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toSource( final Path path,
                            final GuidedDecisionTable52 model ) {
        return sourceServices.getServiceFor( Paths.convert( path ) ).getSource( Paths.convert( path ),
                                                                                model );
    }

    @Override
    public List<ValidationMessage> validate( final Path path,
                                             final GuidedDecisionTable52 content ) {
        try {
            return genericValidator.validate( path,
                                              new ByteArrayInputStream(
                                                      GuidedDTXMLPersistence.getInstance().marshal( content ).getBytes( Charsets.UTF_8 )
                                              ),
                                              FILTER_JAVA,
                                              FILTER_DRL,
                                              FILTER_DSLR,
                                              FILTER_DSL,
                                              FILTER_RDRL,
                                              FILTER_RDSLR,
                                              FILTER_GLOBAL );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public List<Path> listDecisionTablesInProject( final Path path ) {
        try {
            final KieProject project = projectService.resolveProject( path );
            if ( project == null ) {
                return Collections.emptyList();
            }

            final Path projectRoot = project.getRootPath();
            final org.uberfire.java.nio.file.Path nioProjectRoot = Paths.convert( projectRoot );
            final org.uberfire.java.nio.file.Path nioProjectResourcesRoot = nioProjectRoot.resolve( "src/main/resources" );

            final List<Path> paths = findDecisionTables( nioProjectResourcesRoot );
            return paths;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    private List<Path> findDecisionTables( final org.uberfire.java.nio.file.Path nioRootPath ) {
        final List<Path> paths = new ArrayList<>();
        final DirectoryStream<org.uberfire.java.nio.file.Path> directoryStream = ioService.newDirectoryStream( nioRootPath );
        for ( org.uberfire.java.nio.file.Path nioPath : directoryStream ) {
            if ( Files.isDirectory( nioPath ) ) {
                paths.addAll( findDecisionTables( nioPath ) );
            } else {
                final Path path = Paths.convert( nioPath );
                if ( resourceType.accept( path ) ) {
                    paths.add( path );
                }
            }
        }
        return paths;
    }

}
