(function () {
	var form = document.querySelector('[data-explore-location-form]');
	var sortSelects = document.querySelectorAll('[data-sort-select="true"]');
	if (!form && !sortSelects.length) {
		return;
	}

	var latitudeInput = form ? form.querySelector('[data-explore-location-latitude]') : null;
	var longitudeInput = form ? form.querySelector('[data-explore-location-longitude]') : null;
	var status = form ? form.querySelector('[data-explore-location-status]') : null;
	var submitButton = form ? form.querySelector('[data-explore-location-submit]') : null;
	var unavailableMessage = form ? form.dataset.locationUnavailableMessage || '' : '';
	var originalLabel = submitButton ? submitButton.textContent : '';
	var geolocationAvailable = window.isSecureContext && !!navigator.geolocation;

	function setLoading(loading) {
		if (submitButton) {
			submitButton.disabled = loading;
			submitButton.textContent = loading
				? submitButton.dataset.loadingLabel || originalLabel
				: originalLabel;
		}
		sortSelects.forEach(function (select) {
			select.disabled = loading;
		});
	}

	function showUnavailableMessage() {
		if (status && unavailableMessage) {
			status.textContent = unavailableMessage;
		}
	}

	function requestExploreLocation(event) {
		if (!geolocationAvailable || !latitudeInput || !longitudeInput) {
			if (event) {
				event.preventDefault();
			}
			showUnavailableMessage();
			return;
		}

		if (event) {
			event.preventDefault();
		}
		setLoading(true);

		navigator.geolocation.getCurrentPosition(
			function (position) {
				latitudeInput.value = String(position.coords.latitude);
				longitudeInput.value = String(position.coords.longitude);
				form.submit();
			},
			function () {
				showUnavailableMessage();
				setLoading(false);
			},
			{ enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
		);
	}

	if (form) {
		form.addEventListener('submit', requestExploreLocation);
	}

	sortSelects.forEach(function (select) {
		select.querySelectorAll('.sort-panel__item').forEach(function (option) {
			option.addEventListener('click', function (event) {
				var url = new URL(option.href, window.location.href);
				if (url.searchParams.get('sort') !== 'distance') {
					return;
				}

				if (
					form &&
					form.dataset.locationAvailable !== 'true' &&
					geolocationAvailable &&
					latitudeInput &&
					longitudeInput
				) {
					event.preventDefault();
					requestExploreLocation(event);
					return;
				}

				if (form && !geolocationAvailable) {
					event.preventDefault();
					showUnavailableMessage();
				}
			});
		});
	});
})();