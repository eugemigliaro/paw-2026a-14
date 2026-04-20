(function() {
	function toggleVisibility(container) {
		var input = container.querySelector("input");
		var toggle = container.querySelector("[data-password-toggle='true']");
		if (!input || !toggle) {
			return;
		}

		var isVisible = input.type === "text";
		input.type = isVisible ? "password" : "text";
		container.classList.toggle("is-visible", !isVisible);
		toggle.setAttribute("aria-pressed", String(!isVisible));
		toggle.setAttribute(
			"aria-label",
			!isVisible ? toggle.dataset.labelHide : toggle.dataset.labelShow
		);
	}

	document.addEventListener("DOMContentLoaded", function() {
		var passwordFields = document.querySelectorAll("[data-password-visibility='true']");
		passwordFields.forEach(function(container) {
			var toggle = container.querySelector("[data-password-toggle='true']");
			if (!toggle) {
				return;
			}

			toggle.addEventListener("click", function() {
				toggleVisibility(container);
			});
		});
	});
})();
