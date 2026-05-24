(function() {
	"use strict";

	var OPTIONS = {
		padel: [1, 2],
		tennis: [1, 2],
		football: [5, 7, 8, 11],
		basketball: [3, 5],
		other: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
	};

	var DEFAULTS = {
		padel: 2,
		tennis: 1,
		football: 5,
		basketball: 5,
		other: 1
	};

	function updateTeamSizes(sportSelect, teamSizeSelect) {
		var sport = sportSelect.value;
		var allowed = OPTIONS[sport] || OPTIONS.other;
		var currentValue = parseInt(teamSizeSelect.value, 10);
		var nextValue = allowed.indexOf(currentValue) === -1 ? DEFAULTS[sport] || allowed[0] : currentValue;

		teamSizeSelect.innerHTML = "";
		allowed.forEach(function(size) {
			var option = document.createElement("option");
			option.value = String(size);
			option.textContent = String(size);
			option.selected = size === nextValue;
			teamSizeSelect.appendChild(option);
		});
	}

	document.addEventListener("DOMContentLoaded", function() {
		var sportSelect = document.querySelector("[data-tournament-sport-select]");
		var teamSizeSelect = document.querySelector("[data-tournament-team-size-select]");

		if (!sportSelect || !teamSizeSelect) {
			return;
		}

		updateTeamSizes(sportSelect, teamSizeSelect);
		sportSelect.addEventListener("change", function() {
			updateTeamSizes(sportSelect, teamSizeSelect);
		});
	});
})();
