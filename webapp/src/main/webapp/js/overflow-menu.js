(function() {
	function closeMenu(menuRoot, restoreFocus) {
		var trigger = menuRoot.querySelector("[data-overflow-menu-trigger='true']");
		var panel = menuRoot.querySelector("[data-overflow-menu-panel='true']");
		if (!trigger || !panel) {
			return;
		}

		trigger.setAttribute("aria-expanded", "false");
		panel.hidden = true;
		menuRoot.dataset.open = "false";
		if (restoreFocus) {
			trigger.focus();
		}
	}

	function openMenu(menuRoot) {
		var trigger = menuRoot.querySelector("[data-overflow-menu-trigger='true']");
		var panel = menuRoot.querySelector("[data-overflow-menu-panel='true']");
		if (!trigger || !panel) {
			return;
		}

		trigger.setAttribute("aria-expanded", "true");
		panel.hidden = false;
		menuRoot.dataset.open = "true";

		var firstAction = panel.querySelector("a[href], button:not([disabled]), [tabindex='0']");
		if (firstAction) {
			firstAction.focus();
		}
	}

	function toggleMenu(menuRoot) {
		if (menuRoot.dataset.open === "true") {
			closeMenu(menuRoot, false);
			return;
		}

		document.querySelectorAll("[data-overflow-menu='true']").forEach(function(otherMenu) {
			if (otherMenu !== menuRoot) {
				closeMenu(otherMenu, false);
			}
		});
		openMenu(menuRoot);
	}

	function initMenu(menuRoot) {
		var trigger = menuRoot.querySelector("[data-overflow-menu-trigger='true']");
		var panel = menuRoot.querySelector("[data-overflow-menu-panel='true']");
		if (!trigger || !panel) {
			return;
		}

		menuRoot.dataset.open = "false";

		trigger.addEventListener("click", function(event) {
			event.stopPropagation();
			toggleMenu(menuRoot);
		});

		panel.addEventListener("click", function(event) {
			if (event.target.closest("a[href], button[type='submit']")) {
				closeMenu(menuRoot, false);
			}
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		document.querySelectorAll("[data-overflow-menu='true']").forEach(initMenu);

		document.addEventListener("click", function(event) {
			document.querySelectorAll("[data-overflow-menu='true']").forEach(function(menuRoot) {
				if (!menuRoot.contains(event.target)) {
					closeMenu(menuRoot, false);
				}
			});
		});

		document.addEventListener("keydown", function(event) {
			if (event.key !== "Escape") {
				return;
			}

			document.querySelectorAll("[data-overflow-menu='true']").forEach(function(menuRoot) {
				if (menuRoot.dataset.open === "true") {
					closeMenu(menuRoot, true);
				}
			});
		});
	});
})();
