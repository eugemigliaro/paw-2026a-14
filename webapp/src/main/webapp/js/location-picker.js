(function () {
	var CABA_BOUNDS = L.latLngBounds(L.latLng(-34.71, -58.55), L.latLng(-34.52, -58.33));
	var MIN_ZOOM = 12;
	var MAX_ZOOM = 16;

	var container = document.querySelector('[data-location-picker]');
	if (!container) {
		return;
	}

	var mapEl = container.querySelector('[data-location-map]');
	var latInput = document.getElementById('match-location-latitude');
	var lonInput = document.getElementById('match-location-longitude');
	if (!mapEl || !latInput || !lonInput) {
		return;
	}

	var tileTemplate = container.dataset.tileUrlTemplate || '';
	var defaultLat = Number(container.dataset.defaultLatitude || -34.6037);
	var defaultLon = Number(container.dataset.defaultLongitude || -58.3816);
	var defaultZoom = Math.min(Math.max(Number(container.dataset.defaultZoom || 14), MIN_ZOOM), MAX_ZOOM);
	var initialLat = latInput.value ? Number(latInput.value) : null;
	var initialLon = lonInput.value ? Number(lonInput.value) : null;

	var map = L.map(mapEl, {
		minZoom: MIN_ZOOM,
		maxZoom: MAX_ZOOM,
		maxBounds: CABA_BOUNDS,
		maxBoundsViscosity: 1.0,
		zoomControl: false,
		attributionControl: false,
	}).setView(
		initialLat !== null ? [initialLat, initialLon] : [defaultLat, defaultLon],
		defaultZoom
	);

	L.tileLayer(tileTemplate, {
		minZoom: MIN_ZOOM,
		maxZoom: MAX_ZOOM,
		bounds: CABA_BOUNDS,
	}).addTo(map);

	var pinIcon = L.divIcon({
		className: 'location-picker__pin-icon',
		iconSize: [19, 19],
		iconAnchor: [10, 10],
	});

	var marker = null;

	function placeMarker(latlng) {
		if (marker) {
			marker.setLatLng(latlng);
		} else {
			marker = L.marker(latlng, { icon: pinIcon, draggable: true }).addTo(map);
			marker.on('dragend', function () {
				var pos = marker.getLatLng();
				latInput.value = pos.lat.toFixed(7);
				lonInput.value = pos.lng.toFixed(7);
			});
		}
		latInput.value = latlng.lat.toFixed(7);
		lonInput.value = latlng.lng.toFixed(7);
	}

	if (initialLat !== null && initialLon !== null) {
		placeMarker(L.latLng(initialLat, initialLon));
	}

	map.on('click', function (e) {
		placeMarker(e.latlng);
	});

	function updateZoomButtons() {
		var zoomInBtn = container.querySelector('[data-location-zoom-in]');
		var zoomOutBtn = container.querySelector('[data-location-zoom-out]');
		if (zoomInBtn) {
			zoomInBtn.disabled = map.getZoom() >= MAX_ZOOM;
		}
		if (zoomOutBtn) {
			zoomOutBtn.disabled = map.getZoom() <= MIN_ZOOM;
		}
	}

	map.on('zoomend', updateZoomButtons);
	updateZoomButtons();

	var zoomInBtn = container.querySelector('[data-location-zoom-in]');
	var zoomOutBtn = container.querySelector('[data-location-zoom-out]');
	var currentBtn = container.querySelector('[data-location-current]');
	var clearBtn = container.querySelector('[data-location-clear]');

	if (zoomInBtn) {
		zoomInBtn.addEventListener('click', function () { map.zoomIn(); });
	}
	if (zoomOutBtn) {
		zoomOutBtn.addEventListener('click', function () { map.zoomOut(); });
	}
	if (currentBtn) {
		currentBtn.addEventListener('click', function () {
			map.locate({ setView: true, maxZoom: MAX_ZOOM });
		});
		map.on('locationfound', function (e) {
			placeMarker(e.latlng);
		});
	}
	if (clearBtn) {
		clearBtn.addEventListener('click', function () {
			if (marker) {
				map.removeLayer(marker);
				marker = null;
			}
			latInput.value = '';
			lonInput.value = '';
		});
	}
})();
