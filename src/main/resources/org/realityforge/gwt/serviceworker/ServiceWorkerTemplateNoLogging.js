// Note: This file should be the same as ServiceWorkerTemplate.js but with logging stripped out.
var cacheName = '__CACHE_NAME__';
var filesToPreCache = [__PRE_CACHE_RESOURCES__];
var filesToCacheOnAccess = [__MAYBE_CACHE_RESOURCES__];

self.addEventListener('install', function(e) {
  // Immediately replace an existing serviceworker with the current service worker
  e.waitUntil(self.skipWaiting());
  e.waitUntil(
    caches.open(cacheName).then(function(cache) {
      return cache.addAll(filesToPreCache);
    })
  );
});

self.addEventListener('activate', function(e) {
  e.waitUntil(
    caches.keys().then(function(keyList) {
      return Promise.all(keyList.map(function(key) {
        if (key !== cacheName) {
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
  e.respondWith(
    caches.match(e.request).then(function(response) {
      if (response) {
        return response;
      } else {
        // Only attempt to cache resources if they are in the filesToCacheOnAccess list
        if (e.request.url.startsWith(self.registration.scope) &&
            filesToCacheOnAccess[e.request.url.substring(self.registration.scope.length)]) {

          // We call .clone() on the request since we might use it in a call to cache.put() later on.
          // Both fetch() and cache.put() "consume" the request, so we need to make a copy.
          // (see https://fetch.spec.whatwg.org/#dom-request-clone)
          return fetch(e.request.clone()).then(function(networkResponse) {

            // Need to clone response prior to accessing status otherwise
            // accessing status marks it as used and not a candidate for cloning
            var dupResponse = networkResponse.clone();

            if (200 === networkResponse.status) {
              // Only cache successful loads
              caches.open(cacheName).then(function(cache) {
                cache.put(e.request, dupResponse);
              });
            }
            return networkResponse;
          });
        } else {
          return fetch(e.request);
        }
      }
    })
  );
});
