package org.realityforge.gwt.serviceworker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.impl.SelectionInformation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@LinkerOrder( LinkerOrder.Order.POST )
@Shardable
public final class ServiceWorkerLinker
  extends AbstractLinker
{
  @Override
  public String getDescription()
  {
    return "ServiceWorkerLinker";
  }

  @Override
  public ArtifactSet link( @Nonnull final TreeLogger logger,
                           @Nonnull final LinkerContext context,
                           @Nonnull final ArtifactSet artifacts,
                           final boolean onePermutation )
    throws UnableToCompleteException
  {
    if ( onePermutation )
    {
      return perPermutationLink( logger, context, artifacts );
    }
    else
    {
      return perCompileLink( logger, context, artifacts );
    }
  }

  @Nonnull
  private ArtifactSet perCompileLink( @Nonnull final TreeLogger logger,
                                      @Nonnull final LinkerContext context,
                                      @Nonnull final ArtifactSet artifacts )
    throws UnableToCompleteException
  {
    final SortedSet<PermutationArtifact> permutationArtifacts = artifacts.find( PermutationArtifact.class );

    // Get files generated that are specific to particular permutations
    final Set<String> allPermutationFiles = permutationArtifacts.stream()
      .flatMap( artifact -> artifact.getPermutation().getPermutationFiles().stream() )
      .collect( Collectors.toSet() );

    // get all the "candidate" artifacts for caching
    final Set<String> artifactsToCache = getArtifactsToCache( context, artifacts );

    final ArtifactSet results = new ArtifactSet( artifacts );
    for ( final PermutationArtifact permutation : permutationArtifacts )
    {
      // make a copy of all artifacts
      final Set<String> filesForCurrentPermutation = new HashSet<>( artifactsToCache );
      // remove all permutations
      filesForCurrentPermutation.removeAll( allPermutationFiles );
      // add files of the one permutation we are interested in
      // leaving the common stuff for all permutations in...
      for ( final String file : permutation.getPermutation().getPermutationFiles() )
      {
        if ( artifactsToCache.contains( file ) )
        {
          filesForCurrentPermutation.add( file );
        }
      }

      // build manifest
      final Collection<String> externalFiles = getConfiguredStaticFiles( context );
      final String maniFest = writeManifest( logger, externalFiles, filesForCurrentPermutation );
      final String filename =
        permutation.getPermutation().getPermutationName() + Permutation.PERMUTATION_MANIFEST_FILE_ENDING;
      results.add( emitString( logger, maniFest, filename ) );
    }

    return results;
  }

  @Nonnull
  ArtifactSet perPermutationLink( @Nonnull final TreeLogger logger,
                                  @Nonnull final LinkerContext context,
                                  @Nonnull final ArtifactSet artifacts )
    throws UnableToCompleteException
  {
    final Permutation permutation = buildPermutation( context, artifacts );
    if ( null == permutation )
    {
      logger.log( Type.ERROR, "Unable to calculate permutation " );
      throw new UnableToCompleteException();
    }

    final ArtifactSet results = new ArtifactSet( artifacts );
    results.add( new PermutationArtifact( ServiceWorkerLinker.class, permutation ) );
    return results;
  }

  private boolean shouldAddToManifest( @Nonnull final String path )
  {
    return !( path.equals( "compilation-mappings.txt" ) || path.endsWith( ".devmode.js" ) );
  }

  /**
   * Write a manifest file for the given set of artifacts and return it as a
   * string
   *
   * @param staticResources - the static resources of the app, such as index.html file
   * @param cacheResources  the gwt output artifacts like cache.html files
   * @return the manifest as a string
   */
  @Nonnull
  String writeManifest( @Nonnull final TreeLogger logger,
                        @Nonnull final Collection<String> staticResources,
                        @Nonnull final Set<String> cacheResources )
    throws UnableToCompleteException
  {
    final ManifestDescriptor descriptor = new ManifestDescriptor();
    final List<String> cachedResources =
      Stream
        .concat( staticResources.stream(), cacheResources.stream() )
        .sorted()
        .distinct()
        .collect( Collectors.toList() );
    descriptor.getCachedResources().addAll( cachedResources );
    try
    {
      return descriptor.toString();
    }
    catch ( final Exception e )
    {
      logger.log( Type.ERROR, "Error generating manifest: " + e, e );
      throw new UnableToCompleteException();
    }
  }

  @Nonnull
  private Collection<String> getConfiguredStaticFiles( @Nonnull final LinkerContext context )
  {
    return context.getConfigurationProperties()
      .stream()
      .filter( p -> "serviceworker_static_files".equals( p.getName() ) )
      .findFirst()
      .map( ConfigurationProperty::getValues )
      .orElse( Collections.emptyList() );
  }

  /**
   * Return the permutation for a single link step.
   */
  @Nullable
  private Permutation buildPermutation( @Nonnull final LinkerContext context, @Nonnull final ArtifactSet artifacts )
    throws UnableToCompleteException
  {
    Permutation permutation = null;

    for ( final SelectionInformation selectionInformation : artifacts.find( SelectionInformation.class ) )
    {
      final String strongName = selectionInformation.getStrongName();
      if ( null != permutation && !permutation.getPermutationName().equals( strongName ) )
      {
        throw new UnableToCompleteException();
      }
      if ( null == permutation )
      {
        permutation = new Permutation( strongName );
        permutation.getPermutationFiles().addAll( getArtifactsToCache( context, artifacts ) );
      }
    }
    return permutation;
  }

  @Nonnull
  private Set<String> getArtifactsToCache( @Nonnull final LinkerContext context, @Nonnull final ArtifactSet artifacts )
  {
    return artifacts
      .find( EmittedArtifact.class )
      .stream()
      .filter( artifact -> Visibility.Public == artifact.getVisibility() &&
                           shouldAddToManifest( artifact.getPartialPath() ) )
      .map( EmittedArtifact::getPartialPath )
      .collect( Collectors.toSet() );
  }
}
