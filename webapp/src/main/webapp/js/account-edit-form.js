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

	function bindAccountInlineEditor() {
		var form = document.getElementById("account-edit-form");
		if (!form) {
			return;
		}

		var editableFields = form.querySelectorAll(".account-field--editable");
		var fileInput = document.getElementById("account-profile-image");
		var actionsBar = document.getElementById("account-edit-confirm");
		var cancelButton = document.getElementById("account-cancel-button");
		var originalValues = new Map();
		var fileChanged = false;

		if (!editableFields.length || !actionsBar) {
			return;
		}

		editableFields.forEach(function(field) {
			originalValues.set(field, field.value);
		});

		function enterEditMode() {
			editableFields.forEach(function(field) {
				field.removeAttribute("readonly");
				field.classList.remove("account-readonly-control");
			});
		}

		function checkForChanges() {
			var hasAccountChanges = fileChanged;
			editableFields.forEach(function(field) {
				if (field.value !== originalValues.get(field)) {
					hasAccountChanges = true;
				}
			});

			actionsBar.classList.toggle("account-edit-actions__confirm--visible", hasAccountChanges);
		}

		editableFields.forEach(function(field) {
			field.addEventListener("focus", enterEditMode);
			field.addEventListener("click", enterEditMode);
			field.addEventListener("input", checkForChanges);
		});

		if (fileInput) {
			fileInput.addEventListener("change", function() {
				fileChanged = !!fileInput.value;
				checkForChanges();
			});
		}

		if (cancelButton) {
			cancelButton.addEventListener("click", function(event) {
				event.preventDefault();
				editableFields.forEach(function(field) {
					field.value = originalValues.get(field);
					field.setAttribute("readonly", "true");
					field.classList.add("account-readonly-control");
				});
				if (fileInput) {
					fileInput.value = "";
				}
				fileChanged = false;
				actionsBar.classList.remove("account-edit-actions__confirm--visible");
			});
		}
	}

	document.addEventListener("DOMContentLoaded", function() {
		var forms = document.querySelectorAll("form[data-account-edit-form='true']");
		forms.forEach(bindDirtyTracking);
		bindAccountInlineEditor();
	});
})();
