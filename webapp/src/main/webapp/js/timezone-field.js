(function() {
	var timezoneFields = document.querySelectorAll('[data-browser-timezone-field="true"]');
	var timezoneLinks = document.querySelectorAll('[data-browser-timezone-url-link="true"]');
	if ((!timezoneFields.length && !timezoneLinks.length) || !window.Intl || !Intl.DateTimeFormat) {
		return;
	}

	var timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
	if (!timezone) {
		return;
	}

	timezoneFields.forEach(function(field) {
		field.value = timezone;
	});

	timezoneLinks.forEach(function(link) {
		try {
			var url = new URL(link.getAttribute('href'), window.location.href);
			url.searchParams.set('tz', timezone);
			link.setAttribute('href', url.pathname + url.search + url.hash);
		} catch (error) {
			// Ignore non-URL href values.
		}
	});
})();
