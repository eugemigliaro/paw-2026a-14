(function() {
	function bindToggle(toggleRoot) {
		var hiddenInput = toggleRoot.querySelector("[data-events-toggle-input='true']");
		if (hiddenInput) {
			return;
		}

		var eventsButtons = toggleRoot.querySelectorAll(".events-toggle-btn");
		var eventsSlider = toggleRoot.querySelector("[data-events-toggle-slider='true']");
		if (!eventsButtons.length || !eventsSlider) {
			return;
		}

		eventsButtons.forEach(function(button) {
			button.addEventListener("click", function() {
				var value = button.dataset.value;
				var isPast = value === "past";
				var currentUrl = new URL(window.location);

				eventsSlider.classList.toggle("right", isPast);
				eventsButtons.forEach(function(otherButton) {
					otherButton.classList.remove("active");
				});
				button.classList.add("active");

				if (isPast) {
					currentUrl.searchParams.set("filter", "past");
				} else {
					currentUrl.searchParams.delete("filter");
				}
				currentUrl.searchParams.set("page", "1");
				currentUrl.searchParams.delete("startDate");
				currentUrl.searchParams.delete("endDate");
				currentUrl.searchParams.delete("minPrice");
				currentUrl.searchParams.delete("maxPrice");
				window.location.href = currentUrl.toString();
			});
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		document.querySelectorAll("[data-events-toggle='true']").forEach(bindToggle);
	});
})();
