(function () {
	var CABA_BOUNDS = L.latLngBounds(L.latLng(-34.71, -58.55), L.latLng(-34.52, -58.33));
	var MIN_ZOOM = 12;
	var MAX_ZOOM = 16;

	function clampZoom(value) {
		return Math.min(Math.max(Number(value || 14), MIN_ZOOM), MAX_ZOOM);
	}

	function queryInput(container, selector, fallbackId) {
		if (selector) {
			return document.querySelector(selector);
		}
		return fallbackId ? document.getElementById(fallbackId) : null;
	}

	function initLocationPicker(container, options) {
		var settings = options || {};
		var mapEl = container.querySelector('[data-location-map]');
		var latInput = settings.latitudeInput ||
			queryInput(container, container.getAttribute('data-latitude-input'), 'match-location-latitude');
		var lonInput = settings.longitudeInput ||
			queryInput(container, container.getAttribute('data-longitude-input'), 'match-location-longitude');
		if (!mapEl || !latInput || !lonInput || container.dataset.locationPickerReady === 'true') {
			return null;
		}
		container.dataset.locationPickerReady = 'true';

		var tileTemplate = container.dataset.tileUrlTemplate || '';
		var defaultLat = Number(container.dataset.defaultLatitude || -34.6037);
		var defaultLon = Number(container.dataset.defaultLongitude || -58.3816);
		var defaultZoom = clampZoom(container.dataset.defaultZoom);
		var initialLat = latInput.value ? Number(latInput.value) : null;
		var initialLon = lonInput.value ? Number(lonInput.value) : null;
		var currentBtn = container.querySelector('[data-location-current]');
		var zoomInBtn = container.querySelector('[data-location-zoom-in]');
		var zoomOutBtn = container.querySelector('[data-location-zoom-out]');
		var clearBtn = container.querySelector('[data-location-clear]');
		var geolocationAvailable = window.isSecureContext && !!navigator.geolocation;

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

		function emitChange() {
			container.dispatchEvent(
				new CustomEvent('location-picker:change', {
					detail: {
						latitude: latInput.value,
						longitude: lonInput.value,
					},
				})
			);
			if (typeof settings.onChange === 'function') {
				settings.onChange(latInput.value, lonInput.value);
			}
		}

		function placeMarker(latlng) {
			if (marker) {
				marker.setLatLng(latlng);
			} else {
				marker = L.marker(latlng, { icon: pinIcon, draggable: true }).addTo(map);
				marker.on('dragend', function () {
					var pos = marker.getLatLng();
					latInput.value = pos.lat.toFixed(7);
					lonInput.value = pos.lng.toFixed(7);
					emitChange();
				});
			}
			latInput.value = latlng.lat.toFixed(7);
			lonInput.value = latlng.lng.toFixed(7);
			emitChange();
		}

		if (initialLat !== null && initialLon !== null) {
			placeMarker(L.latLng(initialLat, initialLon));
		}

		map.on('click', function (e) {
			placeMarker(e.latlng);
		});

		function updateZoomButtons() {
			if (zoomInBtn) {
				zoomInBtn.disabled = map.getZoom() >= MAX_ZOOM;
			}
			if (zoomOutBtn) {
				zoomOutBtn.disabled = map.getZoom() <= MIN_ZOOM;
			}
		}

		map.on('zoomend', updateZoomButtons);
		updateZoomButtons();

		if (zoomInBtn) {
			zoomInBtn.addEventListener('click', function () {
				map.zoomIn();
			});
		}
		if (zoomOutBtn) {
			zoomOutBtn.addEventListener('click', function () {
				map.zoomOut();
			});
		}
		if (currentBtn) {
			if (!geolocationAvailable) {
				currentBtn.hidden = true;
			} else {
				currentBtn.addEventListener('click', function () {
					map.locate({ setView: true, maxZoom: MAX_ZOOM });
				});
			}
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
				emitChange();
			});
		}

		return {
			invalidateSize: function () {
				map.invalidateSize();
			},
			hasLocation: function () {
				return !!latInput.value && !!lonInput.value;
			},
			submitLocation: function () {
				return {
					latitude: latInput.value,
					longitude: lonInput.value,
				};
			},
		};
	}

	window.MatchPointLocationPicker = {
		init: initLocationPicker,
	};

	document.querySelectorAll('[data-location-picker]').forEach(function (container) {
		if (container.dataset.locationPickerDeferred !== 'true') {
			initLocationPicker(container);
		}
	});
})();
