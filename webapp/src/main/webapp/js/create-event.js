(function() {
	var timezoneInput = document.getElementById("event-timezone");
	if (!timezoneInput || !window.Intl || !Intl.DateTimeFormat) {
		return;
	}

	var timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
	if (timezone) {
		timezoneInput.value = timezone;
	}
})();
