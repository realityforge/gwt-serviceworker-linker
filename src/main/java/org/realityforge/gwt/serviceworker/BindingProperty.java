package org.realityforge.gwt.serviceworker;

import javax.annotation.Nonnull;

public final class BindingProperty
  implements Comparable<BindingProperty>
{
  private final String _name;
  private final String _value;

  public BindingProperty( @Nonnull final String name, @Nonnull final String value )
  {
    _name = name;
    _value = value;
  }

  @Override
  public int compareTo( @Nonnull final BindingProperty that )
  {
    final int result = _name.compareTo( that._name );
    if ( 0 != result )
    {
      return result;
    }
    else
    {
      return _value.compareTo( that._value );
    }
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    else if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    else
    {
      final BindingProperty that = (BindingProperty) o;
      return _name.equals( that._name ) && _value.equals( that._value );
    }
  }

  @Override
  public int hashCode()
  {
    int result = _name.hashCode();
    result = 31 * result + _value.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    return "BindingProperty[name=" + _name + ", value=" + _value + "]";
  }
}
