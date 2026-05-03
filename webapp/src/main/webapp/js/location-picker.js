(function () {
	var TILE_SIZE = 256;
	var MIN_ZOOM = 12;
	var MAX_ZOOM = 16;
	var GRID_RADIUS = 2;
	var MAX_LATITUDE = 85.05112878;
	var DRAG_THRESHOLD = 4;

	var picker = document.querySelector('[data-location-picker]');
	if (!picker) {
		return;
	}

	var map = picker.querySelector('[data-location-map]');
	var tiles = picker.querySelector('[data-location-tiles]');
	var pin = picker.querySelector('[data-location-pin]');
	var latitudeInput = document.getElementById('match-location-latitude');
	var longitudeInput = document.getElementById('match-location-longitude');
	if (!latitudeInput || !longitudeInput) {
		return;
	}
	var currentButton = picker.querySelector('[data-location-current]');
	var clearButton = picker.querySelector('[data-location-clear]');
	var zoomInButton = picker.querySelector('[data-location-zoom-in]');
	var zoomOutButton = picker.querySelector('[data-location-zoom-out]');
	var tileTemplate = picker.dataset.tileUrlTemplate || '';
	var defaultLatitude = Number(picker.dataset.defaultLatitude || 0);
	var defaultLongitude = Number(picker.dataset.defaultLongitude || 0);
	var zoom = clamp(Number(picker.dataset.defaultZoom || 14), MIN_ZOOM, MAX_ZOOM);
	var centerLatitude = Number(latitudeInput.value ? latitudeInput.value : defaultLatitude);
	var centerLongitude = Number(longitudeInput.value ? longitudeInput.value : defaultLongitude);
	var selectedLatitude = latitudeInput.value ? Number(latitudeInput.value) : null;
	var selectedLongitude = longitudeInput.value ? Number(longitudeInput.value) : null;
	var dragState = null;

	function clamp(value, min, max) {
		return Math.max(min, Math.min(max, value));
	}

	function worldSize() {
		return TILE_SIZE * Math.pow(2, zoom);
	}

	function longitudeToWorldPixelX(longitude) {
		return ((longitude + 180) / 360) * worldSize();
	}

	function latitudeToWorldPixelY(latitude) {
		var safeLatitude = clamp(latitude, -MAX_LATITUDE, MAX_LATITUDE);
		var latRad = (safeLatitude * Math.PI) / 180;
		return (
			((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) *
			worldSize()
		);
	}

	function worldPixelXToLongitude(pixelX) {
		return (pixelX / worldSize()) * 360 - 180;
	}

	function worldPixelYToLatitude(pixelY) {
		var mercatorY = Math.PI * (1 - (2 * pixelY) / worldSize());
		return (Math.atan(Math.sinh(mercatorY)) * 180) / Math.PI;
	}

	function topLeftWorldPixel(rect) {
		return {
			x: longitudeToWorldPixelX(centerLongitude) - rect.width / 2,
			y: latitudeToWorldPixelY(centerLatitude) - rect.height / 2,
		};
	}

	function tileUrl(x, y) {
		return tileTemplate
			.replace('{z}', String(zoom))
			.replace('{x}', String(x))
			.replace('{y}', String(y));
	}

	function renderPin(rect, topLeft) {
		if (!pin) {
			return;
		}
		if (selectedLatitude === null || selectedLongitude === null) {
			pin.hidden = true;
			return;
		}
		pin.hidden = false;
		pin.style.left = longitudeToWorldPixelX(selectedLongitude) - topLeft.x + 'px';
		pin.style.top = latitudeToWorldPixelY(selectedLatitude) - topLeft.y + 'px';
	}

	function updateZoomButtons() {
		if (zoomInButton) {
			zoomInButton.disabled = zoom >= MAX_ZOOM;
		}
		if (zoomOutButton) {
			zoomOutButton.disabled = zoom <= MIN_ZOOM;
		}
	}

	function renderTiles() {
		updateZoomButtons();
		if (!map || !tiles || !tileTemplate) {
			return;
		}
		tiles.style.transform = '';
		var rect = map.getBoundingClientRect();
		var topLeft = topLeftWorldPixel(rect);
		var centerTileX = Math.floor(longitudeToWorldPixelX(centerLongitude) / TILE_SIZE);
		var centerTileY = Math.floor(latitudeToWorldPixelY(centerLatitude) / TILE_SIZE);
		var maxTile = Math.pow(2, zoom) - 1;

		tiles.innerHTML = '';
		for (var row = -GRID_RADIUS; row <= GRID_RADIUS; row++) {
			for (var col = -GRID_RADIUS; col <= GRID_RADIUS; col++) {
				var tileX = centerTileX + col;
				var tileY = centerTileY + row;
				var wrappedTileX = ((tileX % (maxTile + 1)) + maxTile + 1) % (maxTile + 1);
				var img = document.createElement('img');
				img.alt = '';
				img.className = 'location-picker__tile';
				img.draggable = false;
				img.style.left = tileX * TILE_SIZE - topLeft.x + 'px';
				img.style.top = tileY * TILE_SIZE - topLeft.y + 'px';
				img.width = TILE_SIZE;
				img.height = TILE_SIZE;
				img.addEventListener('error', function () {
					this.classList.add('location-picker__tile--missing');
					this.removeAttribute('src');
				});
				if (tileY >= 0 && tileY <= maxTile) {
					img.src = tileUrl(wrappedTileX, tileY);
				} else {
					img.classList.add('location-picker__tile--missing');
				}
				tiles.appendChild(img);
			}
		}
		renderPin(rect, topLeft);
	}

	function setPoint(latitude, longitude, recenter) {
		selectedLatitude = clamp(latitude, -90, 90);
		selectedLongitude = clamp(longitude, -180, 180);
		if (recenter) {
			centerLatitude = selectedLatitude;
			centerLongitude = selectedLongitude;
		}
		latitudeInput.value = String(selectedLatitude);
		longitudeInput.value = String(selectedLongitude);
		renderTiles();
	}

	function setPointFromEvent(event) {
		var rect = map.getBoundingClientRect();
		var topLeft = topLeftWorldPixel(rect);
		var worldX = topLeft.x + event.clientX - rect.left;
		var worldY = topLeft.y + event.clientY - rect.top;
		setPoint(worldPixelYToLatitude(worldY), worldPixelXToLongitude(worldX), true);
	}

	function dragOffset(clientX, clientY) {
		return {
			x: clientX - dragState.startX,
			y: clientY - dragState.startY,
		};
	}

	function translateDuringDrag(clientX, clientY) {
		var offset = dragOffset(clientX, clientY);
		if (tiles) {
			tiles.style.transform = 'translate(' + offset.x + 'px, ' + offset.y + 'px)';
		}
		if (pin && !pin.hidden && dragState.pinStartLeft !== null && dragState.pinStartTop !== null) {
			pin.style.left = dragState.pinStartLeft + offset.x + 'px';
			pin.style.top = dragState.pinStartTop + offset.y + 'px';
		}
		if (Math.abs(offset.x) > DRAG_THRESHOLD || Math.abs(offset.y) > DRAG_THRESHOLD) {
			dragState.moved = true;
		}
	}

	function resetDragTranslation() {
		if (tiles) {
			tiles.style.transform = '';
		}
		if (pin && dragState.pinStartLeft !== null && dragState.pinStartTop !== null) {
			pin.style.left = dragState.pinStartLeft + 'px';
			pin.style.top = dragState.pinStartTop + 'px';
		}
	}

	function finalizeDrag(clientX, clientY) {
		var offset = dragOffset(clientX, clientY);
		var centerX = dragState.startCenterX - offset.x;
		var centerY = dragState.startCenterY - offset.y;
		centerLongitude = clamp(worldPixelXToLongitude(centerX), -180, 180);
		centerLatitude = clamp(worldPixelYToLatitude(centerY), -MAX_LATITUDE, MAX_LATITUDE);
		resetDragTranslation();
		renderTiles();
	}

	function changeZoom(delta) {
		zoom = clamp(zoom + delta, MIN_ZOOM, MAX_ZOOM);
		renderTiles();
	}

	if (map) {
		map.addEventListener('dragstart', function (event) {
			event.preventDefault();
		});
		map.addEventListener('pointerdown', function (event) {
			if (event.button !== 0) {
				return;
			}
			event.preventDefault();
			dragState = {
				moved: false,
				pinStartLeft: pin && !pin.hidden ? parseFloat(pin.style.left) : null,
				pinStartTop: pin && !pin.hidden ? parseFloat(pin.style.top) : null,
				startCenterX: longitudeToWorldPixelX(centerLongitude),
				startCenterY: latitudeToWorldPixelY(centerLatitude),
				startX: event.clientX,
				startY: event.clientY,
			};
			map.setPointerCapture(event.pointerId);
		});
		map.addEventListener('pointermove', function (event) {
			if (dragState) {
				translateDuringDrag(event.clientX, event.clientY);
			}
		});
		map.addEventListener('pointerup', function (event) {
			if (dragState && !dragState.moved) {
				resetDragTranslation();
				setPointFromEvent(event);
			} else if (dragState) {
				finalizeDrag(event.clientX, event.clientY);
			}
			dragState = null;
		});
		map.addEventListener('pointercancel', function () {
			if (dragState) {
				resetDragTranslation();
			}
			dragState = null;
		});
		map.addEventListener('keydown', function (event) {
			if (event.key === 'Enter' || event.key === ' ') {
				event.preventDefault();
				setPoint(centerLatitude, centerLongitude, false);
			}
		});
	}

	if (zoomInButton) {
		zoomInButton.addEventListener('click', function () {
			changeZoom(1);
		});
	}

	if (zoomOutButton) {
		zoomOutButton.addEventListener('click', function () {
			changeZoom(-1);
		});
	}

	if (currentButton) {
		currentButton.addEventListener('click', function () {
			if (!navigator.geolocation) {
				return;
			}
			navigator.geolocation.getCurrentPosition(function (position) {
				setPoint(position.coords.latitude, position.coords.longitude, true);
			});
		});
	}

	if (clearButton) {
		clearButton.addEventListener('click', function () {
			selectedLatitude = null;
			selectedLongitude = null;
			latitudeInput.value = '';
			longitudeInput.value = '';
			if (pin) {
				pin.hidden = true;
			}
		});
	}

	renderTiles();
})();
