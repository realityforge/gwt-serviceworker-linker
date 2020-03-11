package org.realityforge.gwt.serviceworker;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

final class Permutation
{
  @Nonnull
  private final String _permutationName;
  @Nonnull
  private final Set<String> _permutationFiles = new HashSet<>();

  Permutation( @Nonnull final String permutationName )
  {
    _permutationName = Objects.requireNonNull( permutationName );
  }

  public int hashCode()
  {
    return _permutationName.hashCode();
  }

  @Override
  public boolean equals( final Object obj )
  {
    if ( !( obj instanceof Permutation ) )
    {
      return false;
    }
    final Permutation other = (Permutation) obj;
    return other._permutationName.equals( _permutationName );
  }

  @Nonnull
  String getPermutationName()
  {
    return _permutationName;
  }

  @Nonnull
  Set<String> getPermutationFiles()
  {
    return _permutationFiles;
  }
}
