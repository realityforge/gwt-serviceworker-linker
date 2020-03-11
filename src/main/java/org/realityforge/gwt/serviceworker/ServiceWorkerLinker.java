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
import com.google.gwt.util.tools.Utility;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
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

    // Resources declared in modules
    final Collection<String> externalResources = getConfiguredStaticFiles( context );

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
      final Set<String> permutationResourceToCache = new HashSet<>( artifactsToCache );
      // remove all permutations
      permutationResourceToCache.removeAll( allPermutationFiles );
      // add files of the one permutation we are interested in
      // leaving the common stuff for all permutations in...
      for ( final String file : permutation.getPermutation().getPermutationFiles() )
      {
        if ( artifactsToCache.contains( file ) )
        {
          permutationResourceToCache.add( file );
        }
      }

      permutationResourceToCache.addAll( externalResources );
      final String permutationName = permutation.getPermutation().getPermutationName();
      // build serviceWorker
      final String serviceWorkerJs =
        writeServiceWorker( logger, context, permutationName, permutationResourceToCache );
      final String filename = permutationName + "-sw.js";
      results.add( emitString( logger, serviceWorkerJs, filename ) );
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
    return !( path.equals( "compilation-mappings.txt" ) || path.endsWith( ".devmode.js" ) || path.contains( ".nocache." ) );
  }

  @Nonnull
  private String writeServiceWorker( @Nonnull final TreeLogger logger,
                                     @Nonnull final LinkerContext context,
                                     @Nonnull final String permutationName,
                                     @Nonnull final Set<String> resources )
    throws UnableToCompleteException
  {
    final String resourceList =
      resources.stream().sorted().distinct().map( v -> "'" + v + "'" ).collect( Collectors.joining( "," ) );
    final StringBuffer serviceWorkerJs =
      readFileToStringBuffer( "org/realityforge/gwt/serviceworker/ServiceWorkerTemplate.js", logger );
    replaceAll( serviceWorkerJs, "__MODULE_NAME__", context.getModuleName() );
    replaceAll( serviceWorkerJs, "__PERMUTATION_NAME__", permutationName );
    replaceAll( serviceWorkerJs, "__RESOURCES__", resourceList );
    return serviceWorkerJs.toString();
    // TODO: Fix up template so it is ES3 compatible so it can be optimized?
    //return context.optimizeJavaScript( logger, serviceWorkerJs.toString() );
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  private StringBuffer readFileToStringBuffer( @Nonnull final String filename,
                                               @Nonnull final TreeLogger logger )
    throws UnableToCompleteException
  {
    try
    {
      return new StringBuffer( Utility.getFileFromClassPath( filename ) );
    }
    catch ( IOException e )
    {
      logger.log( TreeLogger.ERROR, "Unable to read file: " + filename, e );
      throw new UnableToCompleteException();
    }
  }

  private void replaceAll( @Nonnull final StringBuffer buf,
                           @Nonnull final String search,
                           @Nonnull final String replace )
  {
    final int len = search.length();
    for ( int pos = buf.indexOf( search ); pos >= 0; pos = buf.indexOf( search, pos + 1 ) )
    {
      buf.replace( pos, pos + len, replace );
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
