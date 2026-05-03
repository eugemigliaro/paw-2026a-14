(function() {
	function initializeBinaryToggle(toggleRoot) {
		if (!toggleRoot) {
			return null;
		}

		var slider = toggleRoot.querySelector("[data-events-toggle-slider='true']");
		var hiddenInput = toggleRoot.querySelector("[data-events-toggle-input='true']");
		var buttons = toggleRoot.querySelectorAll(".events-toggle-btn");
		var rightValue = toggleRoot.getAttribute("data-events-toggle-right-value");

		if (!slider || !hiddenInput || !buttons.length) {
			return hiddenInput;
		}

		function syncUi(nextValue) {
			buttons.forEach(function(button) {
				var isActive = button.getAttribute("data-value") === nextValue;
				button.classList.toggle("active", isActive);
			});
			slider.classList.toggle("right", nextValue === rightValue);
		}

		buttons.forEach(function(button) {
			button.addEventListener("click", function() {
				var nextValue = button.getAttribute("data-value");
				hiddenInput.value = nextValue;
				syncUi(nextValue);
			});
		});

		syncUi(hiddenInput.value);
		return hiddenInput;
	}

	function initJoinPolicyVisibility(visibilityToggle, joinPolicyField, joinPolicyToggle, visibilityInput, joinPolicyInput) {
		if (!visibilityInput || !joinPolicyField || !joinPolicyInput || !joinPolicyToggle || !visibilityToggle) {
			return;
		}

		function updateJoinPolicyVisibility() {
			var isPrivate = visibilityInput.value === "private";
			joinPolicyField.style.display = isPrivate ? "none" : "";

			if (isPrivate) {
				joinPolicyInput.value = "";
				joinPolicyToggle.querySelectorAll(".events-toggle-btn").forEach(function(button) {
					button.classList.remove("active");
				});
			}
		}

		visibilityToggle.querySelectorAll(".events-toggle-btn").forEach(function(button) {
			button.addEventListener("click", updateJoinPolicyVisibility);
		});
		updateJoinPolicyVisibility();
	}

	function initRecurrenceFields() {
		var recurringCheckbox = document.getElementById("match-recurring");
		var recurrenceSettings = document.getElementById("recurrence-settings");
		var recurrenceEndModeSelect = document.getElementById("match-recurrence-end-mode");
		var recurrenceUntilDateField = document.getElementById("recurrence-until-date-field");
		var recurrenceCountField = document.getElementById("recurrence-count-field");
		var recurrenceUntilDateInput = document.getElementById("match-recurrence-until-date");
		var recurrenceCountInput = document.getElementById("match-recurrence-occurrence-count");

		function updateRecurrenceEndFields() {
			if (!recurrenceEndModeSelect || !recurrenceUntilDateField || !recurrenceCountField) {
				return;
			}

			var mode = recurrenceEndModeSelect.value;
			recurrenceUntilDateField.style.display = mode === "until_date" ? "" : "none";
			recurrenceCountField.style.display = mode === "occurrence_count" ? "" : "none";
			if (mode === "until_date" && recurrenceCountInput) {
				recurrenceCountInput.value = "";
			}
			if (mode === "occurrence_count" && recurrenceUntilDateInput) {
				recurrenceUntilDateInput.value = "";
			}
		}

		function updateRecurrenceSettings() {
			if (!recurringCheckbox || !recurrenceSettings) {
				return;
			}

			recurrenceSettings.style.display = recurringCheckbox.checked ? "" : "none";
			updateRecurrenceEndFields();
		}

		if (recurringCheckbox) {
			recurringCheckbox.addEventListener("change", updateRecurrenceSettings);
		}
		if (recurrenceEndModeSelect) {
			recurrenceEndModeSelect.addEventListener("change", updateRecurrenceEndFields);
		}
		updateRecurrenceSettings();
	}

	function parseLocalDateTime(dateValue, timeValue) {
		if (!dateValue || !timeValue) {
			return null;
		}

		var dateParts = dateValue.split("-").map(Number);
		var timeParts = timeValue.split(":").map(Number);
		return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1], 0, 0);
	}

	function pad(value) {
		return String(value).padStart(2, "0");
	}

	function initDurationPresets() {
		var presetInputs = document.querySelectorAll("input[name='durationPresetUi']");
		var startDateInput = document.getElementById("match-date");
		var startTimeInput = document.getElementById("match-time");
		var endDateInput = document.getElementById("match-end-date");
		var endTimeInput = document.getElementById("match-end-time");
		var activeMode = null;

		if (!presetInputs.length || !startDateInput || !startTimeInput || !endDateInput || !endTimeInput) {
			return;
		}

		function setEndFromPreset(durationMinutes) {
			var start = parseLocalDateTime(startDateInput.value, startTimeInput.value);
			if (!start) {
				return;
			}

			var end = new Date(start.getTime() + durationMinutes * 60 * 1000);
			endDateInput.value = end.getFullYear() + "-" + pad(end.getMonth() + 1) + "-" + pad(end.getDate());
			endTimeInput.value = pad(end.getHours()) + ":" + pad(end.getMinutes());
		}

		function syncPresetUi(activePresetValue) {
			var explicitValue = activePresetValue == null ? null : String(activePresetValue);

			presetInputs.forEach(function(input) {
				var chip = input.closest(".duration-option");
				var isActive = explicitValue != null && input.value === explicitValue;
				input.checked = isActive;
				if (chip) {
					chip.classList.toggle("chip--active", isActive);
				}
			});
		}

		function detectPresetMode() {
			var start = parseLocalDateTime(startDateInput.value, startTimeInput.value);
			var end = parseLocalDateTime(endDateInput.value, endTimeInput.value);
			if (!start || !end) {
				return "custom";
			}

			var diffMinutes = Math.round((end.getTime() - start.getTime()) / 60000);
			if (diffMinutes === 60 || diffMinutes === 90) {
				return String(diffMinutes);
			}
			return "custom";
		}

		presetInputs.forEach(function(input) {
			input.addEventListener("change", function() {
				if (!input.checked) {
					return;
				}

				activeMode = input.value;
				if (activeMode === "custom") {
					syncPresetUi(activeMode);
					return;
				}

				setEndFromPreset(Number(activeMode));
				syncPresetUi(activeMode);
			});
		});

		[startDateInput, startTimeInput].forEach(function(input) {
			input.addEventListener("change", function() {
				if (activeMode === "60" || activeMode === "90") {
					setEndFromPreset(Number(activeMode));
					syncPresetUi(activeMode);
					return;
				}

				if (activeMode !== "custom") {
					activeMode = detectPresetMode();
				}
				syncPresetUi(activeMode);
			});
		});

		[endDateInput, endTimeInput].forEach(function(input) {
			input.addEventListener("change", function() {
				activeMode = detectPresetMode();
				syncPresetUi(activeMode);
			});
		});

		activeMode = detectPresetMode();
		syncPresetUi(activeMode);
	}

	document.addEventListener("DOMContentLoaded", function() {
		var visibilityToggle = document.getElementById("match-visibility-toggle");
		var joinPolicyField = document.getElementById("join-policy-field");
		var joinPolicyToggle = document.getElementById("match-join-policy-toggle");
		var visibilityInput = initializeBinaryToggle(visibilityToggle);
		var joinPolicyInput = initializeBinaryToggle(joinPolicyToggle);

		initJoinPolicyVisibility(visibilityToggle, joinPolicyField, joinPolicyToggle, visibilityInput, joinPolicyInput);
		initRecurrenceFields();
		initDurationPresets();
	});
})();
