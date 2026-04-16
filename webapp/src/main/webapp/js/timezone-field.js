(function() {
	var timezoneFields = document.querySelectorAll('[data-browser-timezone-field="true"]');
	if (!timezoneFields.length || !window.Intl || !Intl.DateTimeFormat) {
		return;
	}

	var timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
	if (!timezone) {
		return;
	}

	timezoneFields.forEach(function(field) {
		field.value = timezone;
	});
})();
