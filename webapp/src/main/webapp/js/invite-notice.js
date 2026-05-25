(function() {
	function isReloadNavigation() {
		if (!window.performance) {
			return false;
		}

		if (typeof window.performance.getEntriesByType === "function") {
			var navigationEntries = window.performance.getEntriesByType("navigation");
			if (navigationEntries.length > 0) {
				return navigationEntries[0].type === "reload";
			}
		}

		return window.performance.navigation && window.performance.navigation.type === 1;
	}

	document.addEventListener("DOMContentLoaded", function() {
		if (!isReloadNavigation()) {
			return;
		}

		document.querySelectorAll("[data-invite-refresh-notice='true']").forEach(function(notice) {
			notice.hidden = true;
		});
	});
})();
