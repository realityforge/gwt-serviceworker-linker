package org.realityforge.gwt.serviceworker;

import java.util.HashSet;
import java.util.Set;

final class Permutation
{
  private final String _permutationName;
  private final Set<String> _permutationFiles = new HashSet<>();

  Permutation( final String permutationName )
  {
    _permutationName = permutationName;
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

  String getPermutationName()
  {
    return _permutationName;
  }

  Set<String> getPermutationFiles()
  {
    return _permutationFiles;
  }
}
