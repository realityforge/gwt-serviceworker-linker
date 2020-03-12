var moduleName = '__MODULE_NAME__';
var moduleLabel = '[' + moduleName +']';
var cacheName = '__CACHE_NAME__';
var filesToPreCache = [__PRE_CACHE_RESOURCES__];
var filesToCacheOnAccess = [__MAYBE_CACHE_RESOURCES__];

self.addEventListener('install', function(e) {
  console.log(moduleLabel, '[ServiceWorker] Install');
  // Immediately replace an existing serviceworker with the current service worker
  e.waitUntil(self.skipWaiting());
  e.waitUntil(
    caches.open(cacheName).then(function(cache) {
      console.log(moduleLabel, '[ServiceWorker] Caching app shell');
      return cache.addAll(filesToPreCache);
    })
  );
});

self.addEventListener('activate', function(e) {
  console.log(moduleLabel, '[ServiceWorker] Activate');
  e.waitUntil(
    caches.keys().then(function(keyList) {
      return Promise.all(keyList.map(function(key) {
        if (key !== cacheName) {
          console.log(moduleLabel, '[ServiceWorker] Removing old cache', key);
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
  console.log(moduleName, '[ServiceWorker] Fetch', e.request.url);
  e.respondWith(
    caches.match(e.request).then(function(response) {
      return response || fetch(e.request);
    })
  );
});
