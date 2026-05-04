(function () {
	var CABA_BOUNDS = L.latLngBounds(L.latLng(-34.71, -58.55), L.latLng(-34.52, -58.33));
	var MIN_ZOOM = 12;
	var MAX_ZOOM = 16;

	var containers = document.querySelectorAll('[data-event-map]');
	if (!containers.length) {
		return;
	}

	var pinIcon = L.divIcon({
		className: 'event-detail-map__pin-icon',
		iconSize: [18, 18],
		iconAnchor: [9, 9],
	});

	Array.prototype.forEach.call(containers, function (container) {
		var lat = Number(container.dataset.latitude);
		var lon = Number(container.dataset.longitude);
		var zoom = Math.min(Math.max(Number(container.dataset.zoom || 14), MIN_ZOOM), MAX_ZOOM);
		var tileTemplate = container.dataset.tileUrlTemplate || '';
		if (!tileTemplate || !isFinite(lat) || !isFinite(lon)) {
			return;
		}

		var mapHost = document.createElement('div');
		mapHost.style.height = '100%';
		mapHost.style.width = '100%';
		container.insertBefore(mapHost, container.firstChild);

		var leafletMap = L.map(mapHost, {
			center: [lat, lon],
			zoom: zoom,
			minZoom: zoom,
			maxZoom: zoom,
			maxBounds: CABA_BOUNDS,
			maxBoundsViscosity: 1.0,
			zoomControl: false,
			attributionControl: false,
			dragging: false,
			scrollWheelZoom: false,
			doubleClickZoom: false,
			touchZoom: false,
			keyboard: false,
			tap: false,
		});

		L.tileLayer(tileTemplate, {
			minZoom: MIN_ZOOM,
			maxZoom: MAX_ZOOM,
			bounds: CABA_BOUNDS,
		}).addTo(leafletMap);

		L.marker([lat, lon], { icon: pinIcon }).addTo(leafletMap);
	});
})();
