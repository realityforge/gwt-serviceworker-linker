var moduleName = '[__MODULE_NAME__]';
var cacheName = '__PERMUTATION_NAME__';
var filesToCache = [__RESOURCES__];

self.addEventListener('install', function(e) {
  console.log(moduleName, '[ServiceWorker] Install');
  // Immediately replace an existing serviceworker with the current service worker
  self.skipWaiting();
  // Claim all existing clients that would be managed by this service worker so
  // that even the first time loading that they will go through the caching
  clients.claim();
  e.waitUntil(
    caches.open(cacheName).then(function(cache) {
      console.log(moduleName, '[ServiceWorker] Caching app shell');
      return cache.addAll(filesToCache);
    })
  );
});

self.addEventListener('activate', function(e) {
  console.log(moduleName, '[ServiceWorker] Activate');
  e.waitUntil(
    caches.keys().then(function(keyList) {
      return Promise.all(keyList.map(function(key) {
        if (key !== cacheName) {
          console.log(moduleName, '[ServiceWorker] Removing old cache', key);
          return caches.delete(key);
        }
      }));
    })
  );
});

self.addEventListener('fetch', function(e) {
  console.log(moduleName, '[ServiceWorker] Fetch', e.request.url);
  e.respondWith(
    caches.match(e.request).then(function(response) {
      return response || fetch(e.request);
    })
  );
});