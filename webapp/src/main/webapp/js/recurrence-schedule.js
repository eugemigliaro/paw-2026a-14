(function() {
	function setExpanded(toggle, extraItems, expanded) {
		extraItems.forEach(function(item) {
			item.hidden = !expanded;
		});

		toggle.setAttribute("aria-expanded", expanded ? "true" : "false");

		var label = expanded ? toggle.dataset.showLessLabel : toggle.dataset.showMoreLabel;
		if (label) {
			toggle.textContent = label;
		}
	}

	function initRecurrenceToggle(toggle) {
		var scheduleId = toggle.getAttribute("aria-controls");
		var schedule = scheduleId ? document.getElementById(scheduleId) : null;
		if (!schedule) {
			return;
		}

		var extraItems = schedule.querySelectorAll("[data-recurrence-extra-date='true']");
		if (extraItems.length === 0) {
			toggle.hidden = true;
			return;
		}

		setExpanded(toggle, extraItems, toggle.getAttribute("aria-expanded") === "true");
		toggle.addEventListener("click", function() {
			setExpanded(toggle, extraItems, toggle.getAttribute("aria-expanded") !== "true");
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		document.querySelectorAll("[data-recurrence-toggle='true']").forEach(initRecurrenceToggle);
	});
})();
