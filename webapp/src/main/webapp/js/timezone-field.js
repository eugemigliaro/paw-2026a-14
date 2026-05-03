(function() {
	var timezoneFields = document.querySelectorAll('[data-browser-timezone-field="true"]');
	var timezoneOptionSelects = document.querySelectorAll('[data-browser-timezone-url-options="true"]');
	if ((!timezoneFields.length && !timezoneOptionSelects.length) || !window.Intl || !Intl.DateTimeFormat) {
		return;
	}

	var timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
	if (!timezone) {
		return;
	}

	timezoneFields.forEach(function(field) {
		field.value = timezone;
	});

	timezoneOptionSelects.forEach(function(select) {
		Array.prototype.forEach.call(select.options, function(option) {
			try {
				var url = new URL(option.value, window.location.href);
				url.searchParams.set('tz', timezone);
				option.value = url.pathname + url.search + url.hash;
			} catch (error) {
				// Ignore non-URL option values.
			}
		});
	});
})();
