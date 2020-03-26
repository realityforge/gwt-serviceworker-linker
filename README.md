# gwt-serviceworker-linker

[![Build Status](https://secure.travis-ci.org/realityforge/gwt-serviceworker-linker.svg?branch=master)](http://travis-ci.org/realityforge/gwt-serviceworker-linker)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.gwt.serviceworker/gwt-serviceworker-linker.svg?label=latest%20release"/>](https://search.maven.org/search?q=g:org.realityforge.gwt.serviceworker)

The [ServiceWorkers](https://www.w3.org/TR/service-workers/) specification enables applications
to take advantage of persistent background processing, including hooks to enable bootstrapping
of web applications while offline. This project attempts to basic AppCache behaviour using
ServiceWorkers.

## Quick Start

The simplest way to serviceworker enable a GWT application is to;

* add the following dependencies into the build system. i.e.

```xml
<dependency>
   <groupId>org.realityforge.gwt.serviceworker</groupId>
   <artifactId>gwt-serviceworker-linker</artifactId>
   <version>0.01</version>
   <scope>provided</scope>
</dependency>
```

* add the following snippet into the .gwt.xml file.

```xml
<module rename-to='myapp'>
  ...

  <inherits name="org.realityforge.gwt.serviceworker.Linker"/>

  <!-- enable the linker that generates the serviceworker javascript -->
  <add-linker name="serviceworker"/>

  <!-- configure all the static files not managed by the GWT compiler -->
  <extend-configuration-property name="serviceworker_static_files" value="./"/>
  <extend-configuration-property name="serviceworker_static_files" value="index.html"/>
</module>
```

* launch the service worker from within the application using [Elemental2](https://github.com/google/elemental2).

```java
import static elemental2.dom.DomGlobal.*;

...

if ( null != navigator.serviceWorker )
{
  navigator.serviceWorker
    .register( GWT.getModuleName() + "-sw.js" )
    .then( registration -> {
      console.log( "ServiceWorker registration successful with scope: " + registration.getScope() );

      // Every minute attempt to update the serviceWorker. If it does update
      // then the "controllerchange" event will fire.
      DomGlobal.setInterval( v -> registration.update(), 60000 );

      return null;
    }, error -> {
      console.log( "ServiceWorker registration failed: ", error );
      return null;
    } );

  navigator.serviceWorker.addEventListener( "controllerchange", e -> {
    // This fires when the service worker controlling this page
    // changes, eg a new worker has skipped waiting and become
    // the new active worker.
    console.log( "ServiceWorker updated ", e );
  } );
}
  ...
```

This should be sufficient to get your application using a serviceworker to cache static assets.
