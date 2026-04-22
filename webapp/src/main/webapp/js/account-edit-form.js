(function() {
	function normalizedValue(field) {
		if (!field) {
			return "";
		}

		if (field.type === "checkbox" || field.type === "radio") {
			return field.checked ? "true" : "false";
		}

		return field.value == null ? "" : field.value;
	}

	function captureInitialValues(form) {
		var fields = form.querySelectorAll("input[name], textarea[name], select[name]");
		fields.forEach(function(field) {
			field.dataset.initialValue = normalizedValue(field);
		});
	}

	function hasChanges(form) {
		var fields = form.querySelectorAll("input[name], textarea[name], select[name]");
		for (var i = 0; i < fields.length; i += 1) {
			var field = fields[i];
			if (normalizedValue(field) !== (field.dataset.initialValue || "")) {
				return true;
			}
		}
		return false;
	}

	function syncSubmitState(form) {
		var submitButton = form.querySelector("button[type='submit'], input[type='submit']");
		if (!submitButton) {
			return;
		}

		var changed = hasChanges(form);
		submitButton.disabled = !changed;
		submitButton.setAttribute("aria-disabled", changed ? "false" : "true");
		submitButton.classList.toggle("is-disabled", !changed);
	}

	function bindDirtyTracking(form) {
		captureInitialValues(form);
		syncSubmitState(form);

		form.addEventListener("input", function() {
			syncSubmitState(form);
		});
		form.addEventListener("change", function() {
			syncSubmitState(form);
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		var forms = document.querySelectorAll("form[data-account-edit-form='true']");
		forms.forEach(bindDirtyTracking);
	});
})();
