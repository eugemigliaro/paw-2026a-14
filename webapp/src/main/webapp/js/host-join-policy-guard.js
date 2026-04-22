(function () {
	function applyPolicyGuard(visibilitySelect, joinPolicySelect) {
		var selectedVisibility = visibilitySelect.value;
		var directOption = joinPolicySelect.querySelector("option[value='direct']");
		var approvalOption = joinPolicySelect.querySelector("option[value='approval_required']");

		if (!directOption || !approvalOption) {
			return;
		}

		if (selectedVisibility === "private") {
			directOption.disabled = true;
			joinPolicySelect.value = "approval_required";
			return;
		}

		directOption.disabled = false;
	}

	function init() {
		var visibilitySelect = document.getElementById("match-visibility");
		var joinPolicySelect = document.getElementById("match-join-policy");

		if (!visibilitySelect || !joinPolicySelect) {
			return;
		}

		applyPolicyGuard(visibilitySelect, joinPolicySelect);
		visibilitySelect.addEventListener("change", function () {
			applyPolicyGuard(visibilitySelect, joinPolicySelect);
		});
	}

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
		return;
	}

	init();
})();
