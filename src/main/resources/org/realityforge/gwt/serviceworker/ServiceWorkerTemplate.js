var moduleName = '__MODULE_NAME__';
var moduleLabel = '[' + moduleName + ']';
var cacheName = '__CACHE_NAME__';
var filesToPreCache = [__PRE_CACHE_RESOURCES__];
var filesToCacheOnAccess = [__MAYBE_CACHE_RESOURCES__];

self.addEventListener('install', function(e) {
  if (__LOG_LEVEL__ > 0) {
    console.log(moduleLabel, '[ServiceWorker] Install');
  }
  // Immediately replace an existing serviceworker with the current service worker
  e.waitUntil(self.skipWaiting());
  e.waitUntil(
    caches.open(cacheName).then(function(cache) {
      if (__LOG_LEVEL__ > 1) {
        console.log(moduleLabel, '[ServiceWorker] Caching app shell');
      }
      return cache.addAll(filesToPreCache);
    })
  );
});

self.addEventListener('activate', function(e) {
  if (__LOG_LEVEL__ > 0) {
    console.log(moduleLabel, '[ServiceWorker] Activate');
  }
  e.waitUntil(
    caches.keys().then(function(keyList) {
      return Promise.all(keyList.map(function(key) {
        if (key !== cacheName) {
          if (__LOG_LEVEL__ > 0) {
            console.log(moduleLabel, '[ServiceWorker] Removing old cache', key);
          }
          // Do this rather than caches.delete(key) as ES3 optimizer in GWT thinks delete is a keyword
          return caches['delete'](key);
        }
      }));
    })
  );
  // Claim all existing clients that would be managed by this service worker so
  // that even the first time loading that they will go through the sw caching
  e.waitUntil(clients.claim());
});

self.addEventListener('fetch', function(e) {
  if (__LOG_LEVEL__ > 1) {
    console.log(moduleName, '[ServiceWorker] Fetch: ', e.request.url);
  }
  e.respondWith(
    caches.match(e.request).then(function(response) {
      if (response) {
        if (__LOG_LEVEL__ > 1) {
          console.log(moduleName, '[ServiceWorker] Cached resource returned from fetch: ', e.request.url);
        }
        return response;
      } else {
        // Only attempt to cache resources if they are in the filesToCacheOnAccess list
        if (e.request.url.startsWith(self.registration.scope) &&
            filesToCacheOnAccess[e.request.url.substring(self.registration.scope.length)]) {

          // We call .clone() on the request since we might use it in a call to cache.put() later on.
          // Both fetch() and cache.put() "consume" the request, so we need to make a copy.
          // (see https://fetch.spec.whatwg.org/#dom-request-clone)
          return fetch(e.request.clone()).then(function(networkResponse) {
            if (__LOG_LEVEL__ > 1) {
              console.log(moduleName, '[ServiceWorker] Fetching cacheOnAccess resource: ', e.request.url);
            }

            // Need to clone response prior to accessing status otherwise
            // accessing status marks it as used and not a candidate for cloning
            var dupResponse = networkResponse.clone();

            if (200 === networkResponse.status) {
              // Only cache successful loads
              if (__LOG_LEVEL__ > 1) {
                console.log(moduleName, '[ServiceWorker] Caching and returning ' +
                                        'cacheOnAccess resource: ', e.request.url);
              }
              caches.open(cacheName).then(function(cache) {
                cache.put(e.request, dupResponse);
              });
            } else {
              if (__LOG_LEVEL__ > 1) {
                console.log(moduleName, '[ServiceWorker] Fetch resulted in non-200 response code. ' +
                                        'Skipping cache of cacheOnAccess resource: ', e.request.url);
              }
            }
            return networkResponse;
          });
        } else {
          if (__LOG_LEVEL__ > 1) {
            console.log(moduleName, '[ServiceWorker] Fetch passed through as resource is ' +
                                    'outside serviceworker scope: ', e.request.url);
          }
          return fetch(e.request);
        }
      }
    })
  );
});
