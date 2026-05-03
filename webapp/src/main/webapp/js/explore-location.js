(function () {
	var form = document.querySelector('[data-explore-location-form]');
	if (!form) {
		return;
	}

	var latitudeInput = form.querySelector('[data-explore-location-latitude]');
	var longitudeInput = form.querySelector('[data-explore-location-longitude]');
	var submitButton = form.querySelector('[data-explore-location-submit]');
	var status = form.querySelector('[data-explore-location-status]');
	var originalLabel = submitButton ? submitButton.textContent : '';

	form.addEventListener('submit', function (event) {
		if (!navigator.geolocation || !latitudeInput || !longitudeInput) {
			return;
		}
		event.preventDefault();
		if (submitButton) {
			submitButton.disabled = true;
			submitButton.textContent = submitButton.dataset.loadingLabel || originalLabel;
		}

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
				if (submitButton) {
					submitButton.disabled = false;
					submitButton.textContent = originalLabel;
				}
			},
			{ enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
		);
	});
})();
