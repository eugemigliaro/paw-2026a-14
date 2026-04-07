(function() {
	function disableSubmitButtons(form) {
		var submitButtons = form.querySelectorAll("button[type='submit'], input[type='submit']");
		submitButtons.forEach(function(button) {
			if (!button.disabled) {
				button.dataset.originalLabel = button.tagName === "INPUT" ? button.value : button.textContent;
			}
			button.disabled = true;
			button.setAttribute("aria-disabled", "true");
		});
	}

	function updateLoadingLabel(form) {
		var loadingLabel = form.dataset.submitLoadingLabel;
		if (!loadingLabel) {
			return;
		}

		var submitButton = form.querySelector("button[type='submit'], input[type='submit']");
		if (!submitButton) {
			return;
		}

		if (submitButton.tagName === "INPUT") {
			submitButton.value = loadingLabel;
			return;
		}

		submitButton.textContent = loadingLabel;
	}

	function guardFormSubmit(form) {
		form.addEventListener("submit", function(event) {
			if (form.dataset.submitting === "true") {
				event.preventDefault();
				return;
			}

			form.dataset.submitting = "true";
			form.setAttribute("aria-busy", "true");
			updateLoadingLabel(form);
			disableSubmitButtons(form);
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		var guardedForms = document.querySelectorAll("form[data-submit-guard='true']");
		guardedForms.forEach(guardFormSubmit);
	});
})();
