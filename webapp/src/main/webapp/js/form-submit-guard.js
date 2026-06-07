(function() {
	function disableSubmitButtons(form) {
		var submitButtons = form.querySelectorAll("button[type='submit'], input[type='submit']");
		submitButtons.forEach(function(button) {
			button.dataset.originalDisabled = button.disabled ? "true" : "false";
			button.dataset.originalLabel = button.tagName === "INPUT" ? button.value : button.textContent;
			button.disabled = true;
			button.setAttribute("aria-disabled", "true");
		});
	}

	function restoreSubmitButtons(form) {
		var submitButtons = form.querySelectorAll("button[type='submit'], input[type='submit']");
		submitButtons.forEach(function(button) {
			if (button.dataset.originalLabel) {
				if (button.tagName === "INPUT") {
					button.value = button.dataset.originalLabel;
				} else {
					button.textContent = button.dataset.originalLabel;
				}
			}

			if (button.dataset.originalDisabled) {
				button.disabled = button.dataset.originalDisabled === "true";
				button.setAttribute("aria-disabled", button.disabled ? "true" : "false");
			}
		});
	}

	function resetSubmitGuard(form) {
		delete form.dataset.submitting;
		form.removeAttribute("aria-busy");
		restoreSubmitButtons(form);
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
			var confirmMessage = form.dataset.submitConfirmMessage;
			if (confirmMessage && !window.confirm(confirmMessage)) {
				event.preventDefault();
				return;
			}

			if (form.dataset.submitting === "true") {
				event.preventDefault();
				return;
			}

			form.dataset.submitting = "true";
			form.setAttribute("aria-busy", "true");
			disableSubmitButtons(form);
			updateLoadingLabel(form);
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		var guardedForms = document.querySelectorAll("form[data-submit-guard='true']");
		guardedForms.forEach(guardFormSubmit);
	});

	window.addEventListener("pageshow", function() {
		var guardedForms = document.querySelectorAll("form[data-submit-guard='true']");
		guardedForms.forEach(resetSubmitGuard);
	});
})();
