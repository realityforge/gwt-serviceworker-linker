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
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;
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
      return perPermutationLink( logger, artifacts );
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

    final Set<String> commonResources =
      permutationArtifacts.isEmpty() ?
      new HashSet<>() :
      new HashSet<>( permutationArtifacts.last().getPermutation().getPermutationFiles() );

    // This contains all the non-permutation artifacts i.e. assets in public dirs etc.
    final Set<String> artifactsToCache = getArtifactsToCache( artifacts );

    for ( final PermutationArtifact permutationArtifact : permutationArtifacts )
    {
      final Set<String> files = permutationArtifact.getPermutation().getPermutationFiles();
      artifactsToCache.removeAll( files );
      commonResources.removeIf( f -> !files.contains( f ) );
    }
    commonResources.addAll( externalResources );
    commonResources.addAll( artifactsToCache );

    final Set<String> permutationResources = new HashSet<>();
    for ( final PermutationArtifact permutationArtifact : permutationArtifacts )
    {
      final Set<String> files = permutationArtifact.getPermutation().getPermutationFiles();
      for ( String file : files )
      {
        if ( !commonResources.contains( file ) )
        {
          permutationResources.add( file );
        }
      }
    }

    final ArtifactSet results = new ArtifactSet( artifacts );

    final String cacheName =
      StringUtils.toHexString( Md5Utils.getMd5Digest( String.join( "|", commonResources ) +
                                                      String.join( "|", permutationResources ) ) );
    // build serviceWorker
    final String serviceWorkerJs =
      writeServiceWorker( logger, context, cacheName, commonResources, permutationResources );
    final String filename = context.getModuleName() + "-sw.js";
    results.add( emitString( logger, serviceWorkerJs, filename ) );

    return results;
  }

  @Nonnull
  ArtifactSet perPermutationLink( @Nonnull final TreeLogger logger, @Nonnull final ArtifactSet artifacts )
    throws UnableToCompleteException
  {
    final Permutation permutation = buildPermutation( artifacts );
    if ( null == permutation )
    {
      logger.log( Type.ERROR, "Unable to calculate permutation " );
      throw new UnableToCompleteException();
    }

    final ArtifactSet results = new ArtifactSet( artifacts );
    results.add( new PermutationArtifact( ServiceWorkerLinker.class, permutation ) );
    return results;
  }

  @Nonnull
  private String writeServiceWorker( @Nonnull final TreeLogger logger,
                                     @Nonnull final LinkerContext context,
                                     @Nonnull final String cacheName,
                                     @Nonnull final Set<String> commonResources,
                                     @Nonnull final Set<String> permutationResources )
    throws UnableToCompleteException
  {
    final StringBuffer serviceWorkerJs =
      readFileToStringBuffer( "org/realityforge/gwt/serviceworker/ServiceWorkerTemplate.js", logger );
    replaceAll( serviceWorkerJs, "__MODULE_NAME__", context.getModuleName() );
    replaceAll( serviceWorkerJs, "__CACHE_NAME__", cacheName );
    replaceAll( serviceWorkerJs, "__PRE_CACHE_RESOURCES__", toJsArrayContents( commonResources ) );
    replaceAll( serviceWorkerJs, "__MAYBE_CACHE_RESOURCES__", toJsArrayContents( permutationResources ) );
    //return context.optimizeJavaScript( logger, serviceWorkerJs.toString() );
    return serviceWorkerJs.toString();
  }

  @Nonnull
  private String toJsArrayContents( @Nonnull final Set<String> resources )
  {
    return resources.stream().sorted().distinct().map( v -> "'" + v + "'" ).collect( Collectors.joining( "," ) );
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
  private Permutation buildPermutation( @Nonnull final ArtifactSet artifacts )
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
        permutation.getPermutationFiles().addAll( getArtifactsToCache( artifacts ) );
      }
    }
    return permutation;
  }

  @Nonnull
  private Set<String> getArtifactsToCache( @Nonnull final ArtifactSet artifacts )
  {
    return artifacts
      .find( EmittedArtifact.class )
      .stream()
      .filter( this::shouldAddToManifest )
      .map( EmittedArtifact::getPartialPath )
      .collect( Collectors.toSet() );
  }

  private boolean shouldAddToManifest( @Nonnull final EmittedArtifact artifact )
  {
    return Visibility.Public == artifact.getVisibility() &&
           shouldAddToManifest( artifact.getPartialPath() );
  }

  private boolean shouldAddToManifest( @Nonnull final String path )
  {
    return !( path.equals( "compilation-mappings.txt" ) ||
              path.endsWith( ".devmode.js" ) ||
              path.contains( ".nocache." ) );
  }
}
