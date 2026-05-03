(function () {
	var picker = document.querySelector('[data-location-picker]');
	if (!picker) {
		return;
	}

	var map = picker.querySelector('[data-location-map]');
	var tiles = picker.querySelector('[data-location-tiles]');
	var pin = picker.querySelector('[data-location-pin]');
	var status = picker.querySelector('[data-location-status]');
	var latitudeInput = document.getElementById('match-location-latitude');
	var longitudeInput = document.getElementById('match-location-longitude');
	var currentButton = picker.querySelector('[data-location-current]');
	var clearButton = picker.querySelector('[data-location-clear]');
	var tileTemplate = picker.dataset.tileUrlTemplate || '';
	var defaultLatitude = Number(picker.dataset.defaultLatitude || 0);
	var defaultLongitude = Number(picker.dataset.defaultLongitude || 0);
	var zoom = Number(picker.dataset.defaultZoom || 12);
	var centerLatitude = Number(latitudeInput && latitudeInput.value ? latitudeInput.value : defaultLatitude);
	var centerLongitude = Number(longitudeInput && longitudeInput.value ? longitudeInput.value : defaultLongitude);

	function clamp(value, min, max) {
		return Math.max(min, Math.min(max, value));
	}

	function tileUrl(x, y) {
		return tileTemplate
			.replace('{z}', String(zoom))
			.replace('{x}', String(x))
			.replace('{y}', String(y));
	}

	function lngToTile(longitude) {
		return Math.floor(((longitude + 180) / 360) * Math.pow(2, zoom));
	}

	function latToTile(latitude) {
		var latRad = (latitude * Math.PI) / 180;
		return Math.floor(
			((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) *
				Math.pow(2, zoom)
		);
	}

	function renderTiles() {
		if (!tiles || !tileTemplate) {
			return;
		}
		var centerX = lngToTile(centerLongitude);
		var centerY = latToTile(centerLatitude);
		tiles.innerHTML = '';
		for (var row = -1; row <= 1; row++) {
			for (var col = -1; col <= 1; col++) {
				var img = document.createElement('img');
				img.alt = '';
				img.src = tileUrl(centerX + col, centerY + row);
				img.className = 'location-picker__tile';
				tiles.appendChild(img);
			}
		}
	}

	function setPoint(latitude, longitude) {
		var safeLatitude = clamp(latitude, -90, 90);
		var safeLongitude = clamp(longitude, -180, 180);
		centerLatitude = safeLatitude;
		centerLongitude = safeLongitude;
		latitudeInput.value = String(safeLatitude);
		longitudeInput.value = String(safeLongitude);
		if (pin) {
			pin.hidden = false;
			pin.style.left = '50%';
			pin.style.top = '50%';
		}
		renderTiles();
	}

	function setPointFromEvent(event) {
		var rect = map.getBoundingClientRect();
		var xRatio = (event.clientX - rect.left) / rect.width - 0.5;
		var yRatio = (event.clientY - rect.top) / rect.height - 0.5;
		var longitude = centerLongitude + xRatio * 0.08;
		var latitude = centerLatitude - yRatio * 0.08;
		setPoint(latitude, longitude);
		if (pin) {
			pin.style.left = event.clientX - rect.left + 'px';
			pin.style.top = event.clientY - rect.top + 'px';
		}
	}

	if (map) {
		map.addEventListener('click', setPointFromEvent);
		map.addEventListener('keydown', function (event) {
			if (event.key === 'Enter' || event.key === ' ') {
				event.preventDefault();
				setPoint(centerLatitude, centerLongitude);
			}
		});
	}

	if (currentButton) {
		currentButton.addEventListener('click', function () {
			if (!navigator.geolocation) {
				return;
			}
			navigator.geolocation.getCurrentPosition(function (position) {
				setPoint(position.coords.latitude, position.coords.longitude);
			});
		});
	}

	if (clearButton) {
		clearButton.addEventListener('click', function () {
			latitudeInput.value = '';
			longitudeInput.value = '';
			if (pin) {
				pin.hidden = true;
			}
			if (status) {
				status.textContent = '';
			}
		});
	}

	if (latitudeInput && longitudeInput && latitudeInput.value && longitudeInput.value) {
		setPoint(Number(latitudeInput.value), Number(longitudeInput.value));
	} else {
		renderTiles();
	}
})();
