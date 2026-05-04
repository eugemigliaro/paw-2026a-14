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
	var originalLabel = submitButton ? submitButton.textContent : '';

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

	function requestExploreLocation(event) {
		if (!navigator.geolocation || !latitudeInput || !longitudeInput) {
			return;
		}
		event.preventDefault();
		setLoading(true);

		navigator.geolocation.getCurrentPosition(
			function (position) {
				latitudeInput.value = String(position.coords.latitude);
				longitudeInput.value = String(position.coords.longitude);
				form.submit();
			},
			function () {
				if (status) {
					status.textContent = '';
				}
				setLoading(false);
			},
			{ enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
		);
	}

	if (form) {
		form.addEventListener('submit', requestExploreLocation);
	}

	sortSelects.forEach(function (select) {
		select.addEventListener('change', function (event) {
			if (!select.value) {
				return;
			}
			var url = new URL(select.value, window.location.href);
			if (url.searchParams.get('sort') === 'distance' && form && form.dataset.locationAvailable !== 'true') {
				requestExploreLocation(event);
				return;
			}
			window.location.href = select.value;
		});
	});
})();
