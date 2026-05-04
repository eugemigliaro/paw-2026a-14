(function () {
	var form = document.querySelector('[data-explore-location-form]');
	var sortSelects = document.querySelectorAll('[data-sort-select="true"]');
	if (!form && !sortSelects.length) {
		return;
	}

	var latitudeInput = form ? form.querySelector('[data-explore-location-latitude]') : null;
	var longitudeInput = form ? form.querySelector('[data-explore-location-longitude]') : null;
	var modal = document.querySelector('[data-explore-location-modal]');
	var pickerContainer = modal ? modal.querySelector('[data-location-picker]') : null;
	var confirmButton = modal ? modal.querySelector('[data-explore-location-confirm]') : null;
	var cancelButtons = modal ? modal.querySelectorAll('[data-explore-location-cancel]') : [];
	var geolocationAvailable = window.isSecureContext && !!navigator.geolocation;
	var manualPicker = null;
	var activeTrigger = null;

	function setLoading(loading) {
		sortSelects.forEach(function (select) {
			select.querySelectorAll('.sort-panel__item').forEach(function (option) {
				option.setAttribute('aria-disabled', loading ? 'true' : 'false');
			});
		});
	}

	function hasFormCoordinates() {
		return !!latitudeInput && !!longitudeInput && !!latitudeInput.value && !!longitudeInput.value;
	}

	function updateConfirmState() {
		if (confirmButton) {
			confirmButton.disabled = !hasFormCoordinates();
		}
	}

	function closeManualPicker() {
		if (!modal) {
			return;
		}
		modal.hidden = true;
		document.body.classList.remove('modal-open');
		if (activeTrigger) {
			activeTrigger.focus();
			activeTrigger = null;
		}
	}

	function openManualPicker(trigger) {
		if (!modal || !pickerContainer || !window.MatchPointLocationPicker) {
			return false;
		}
		activeTrigger = trigger || null;
		modal.hidden = false;
		document.body.classList.add('modal-open');
		if (!manualPicker) {
			manualPicker = window.MatchPointLocationPicker.init(pickerContainer);
			pickerContainer.addEventListener('location-picker:change', updateConfirmState);
		}
		window.setTimeout(function () {
			if (manualPicker) {
				manualPicker.invalidateSize();
			}
		}, 0);
		updateConfirmState();
		if (confirmButton) {
			confirmButton.focus();
		}
		return true;
	}

	function submitExploreLocation() {
		if (form && hasFormCoordinates()) {
			form.submit();
		}
	}

	function requestBrowserLocation(trigger) {
		if (!geolocationAvailable || !latitudeInput || !longitudeInput) {
			openManualPicker(trigger);
			return;
		}

		setLoading(true);
		navigator.geolocation.getCurrentPosition(
			function (position) {
				latitudeInput.value = String(position.coords.latitude);
				longitudeInput.value = String(position.coords.longitude);
				form.submit();
			},
			function (error) {
				setLoading(false);
				if (error && error.code === error.PERMISSION_DENIED) {
					return;
				}
				openManualPicker(trigger);
			},
			{ enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
		);
	}

	if (form) {
		form.addEventListener('submit', function (event) {
			event.preventDefault();
			requestBrowserLocation(null);
		});
	}

	sortSelects.forEach(function (select) {
		select.querySelectorAll('.sort-panel__item').forEach(function (option) {
			option.addEventListener('click', function (event) {
				var url = new URL(option.href, window.location.href);
				if (url.searchParams.get('sort') !== 'distance') {
					return;
				}
				if (!form || form.dataset.locationAvailable === 'true') {
					return;
				}
				if (!geolocationAvailable && !modal) {
					return;
				}
				event.preventDefault();
				requestBrowserLocation(option);
			});
		});
	});

	if (confirmButton) {
		confirmButton.addEventListener('click', function () {
			submitExploreLocation();
		});
	}

	cancelButtons.forEach(function (button) {
		button.addEventListener('click', closeManualPicker);
	});

	if (modal) {
		modal.addEventListener('click', function (event) {
			if (event.target === modal) {
				closeManualPicker();
			}
		});
		document.addEventListener('keydown', function (event) {
			if (!modal.hidden && event.key === 'Escape') {
				closeManualPicker();
			}
		});
	}
})();
