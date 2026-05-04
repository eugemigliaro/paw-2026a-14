(function () {
	var TILE_SIZE = 256;
	var MIN_ZOOM = 12;
	var MAX_ZOOM = 16;
	var GRID_RADIUS = 1;
	var MAX_LATITUDE = 85.05112878;

	var maps = document.querySelectorAll('[data-event-map]');
	if (!maps.length) {
		return;
	}

	function clamp(value, min, max) {
		return Math.max(min, Math.min(max, value));
	}

	function worldSize(zoom) {
		return TILE_SIZE * Math.pow(2, zoom);
	}

	function longitudeToWorldPixelX(longitude, zoom) {
		return ((longitude + 180) / 360) * worldSize(zoom);
	}

	function latitudeToWorldPixelY(latitude, zoom) {
		var safeLatitude = clamp(latitude, -MAX_LATITUDE, MAX_LATITUDE);
		var latRad = (safeLatitude * Math.PI) / 180;
		return (
			((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) *
			worldSize(zoom)
		);
	}

	function tileUrl(template, x, y, zoom) {
		return template
			.replace('{z}', String(zoom))
			.replace('{x}', String(x))
			.replace('{y}', String(y));
	}

	function renderMap(map) {
		var tiles = map.querySelector('[data-event-map-tiles]');
		var tileTemplate = map.dataset.tileUrlTemplate || '';
		var latitude = Number(map.dataset.latitude);
		var longitude = Number(map.dataset.longitude);
		var zoom = clamp(Number(map.dataset.zoom || 14), MIN_ZOOM, MAX_ZOOM);
		if (!tiles || !tileTemplate || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
			return;
		}

		var rect = map.getBoundingClientRect();
		var centerX = longitudeToWorldPixelX(longitude, zoom);
		var centerY = latitudeToWorldPixelY(latitude, zoom);
		var topLeftX = centerX - rect.width / 2;
		var topLeftY = centerY - rect.height / 2;
		var centerTileX = Math.floor(centerX / TILE_SIZE);
		var centerTileY = Math.floor(centerY / TILE_SIZE);
		var maxTile = Math.pow(2, zoom) - 1;

		tiles.innerHTML = '';
		for (var row = -GRID_RADIUS; row <= GRID_RADIUS; row++) {
			for (var col = -GRID_RADIUS; col <= GRID_RADIUS; col++) {
				var tileX = centerTileX + col;
				var tileY = centerTileY + row;
				var wrappedTileX = ((tileX % (maxTile + 1)) + maxTile + 1) % (maxTile + 1);
				var img = document.createElement('img');
				img.alt = '';
				img.className = 'event-detail-map__tile';
				img.style.left = tileX * TILE_SIZE - topLeftX + 'px';
				img.style.top = tileY * TILE_SIZE - topLeftY + 'px';
				img.width = TILE_SIZE;
				img.height = TILE_SIZE;
				img.addEventListener('error', function () {
					this.classList.add('event-detail-map__tile--missing');
					this.removeAttribute('src');
				});
				if (tileY >= 0 && tileY <= maxTile) {
					img.src = tileUrl(tileTemplate, wrappedTileX, tileY, zoom);
				} else {
					img.classList.add('event-detail-map__tile--missing');
				}
				tiles.appendChild(img);
			}
		}
	}

	Array.prototype.forEach.call(maps, renderMap);
	window.addEventListener('resize', function () {
		Array.prototype.forEach.call(maps, renderMap);
	});
})();
