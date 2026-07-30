(function () {
	var DAYS_SHORT = {
		en: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
		es: ["dom", "lun", "mar", "mi\u00e9", "jue", "vie", "s\u00e1b"],
	};
	var MONTHS_LONG = {
		en: [
			"January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December",
		],
		es: [
			"enero", "febrero", "marzo", "abril", "mayo", "junio",
			"julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
		],
	};

	function initPicker(container) {
		var lang = container.getAttribute("data-lang") || "en";
		var hiddenInput = container.querySelector("[data-dpicker-iso]");
		var displayInput = container.querySelector("[data-dpicker-display]");
		var btn = container.querySelector("[data-dpicker-btn]");
		var popup = container.querySelector("[data-dpicker-popup]");
		var minStr = container.getAttribute("data-min") || "";
		var maxStr = container.getAttribute("data-max") || "";
		var minDate = minStr ? parseISODate(minStr) : null;
		var maxDate = maxStr ? parseISODate(maxStr) : null;
		var selectedDate = hiddenInput.value ? parseISODate(hiddenInput.value) : null;
		var viewYear = selectedDate ? selectedDate.getFullYear() : today().getFullYear();
		var viewMonth = selectedDate ? selectedDate.getMonth() : today().getMonth();

		popup.innerHTML = "";
		updateDisplay();

		function open() {
			viewYear = selectedDate ? selectedDate.getFullYear() : today().getFullYear();
			viewMonth = selectedDate ? selectedDate.getMonth() : today().getMonth();
			renderPopup();
			popup.classList.add("dpicker__popup--open");
		}

		function close() {
			popup.classList.remove("dpicker__popup--open");
		}

		function updateDisplay() {
			if (selectedDate) {
				displayInput.value = formatDisplay(selectedDate, lang);
			} else {
				displayInput.value = "";
			}
		}

		function renderPopup() {
			var html = "";
			html += '<div class="dpicker__header">';
			html +=
				'<button type="button" class="dpicker__nav" data-dpicker-prev="true" aria-label="Previous month"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg></button>';
			html +=
				'<span class="dpicker__month-label">' +
				MONTHS_LONG[lang][viewMonth] +
				" " +
				viewYear +
				"</span>";
			html +=
				'<button type="button" class="dpicker__nav" data-dpicker-next="true" aria-label="Next month"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></button>';
			html += "</div>";
			html += '<table class="dpicker__grid"><thead><tr>';
			var shortDays = DAYS_SHORT[lang] || DAYS_SHORT.en;
			for (var d = 0; d < 7; d++) {
				html +=
					'<th class="dpicker__weekday">' + shortDays[d] + "</th>";
			}
			html += "</tr></thead><tbody>";

			var first = new Date(viewYear, viewMonth, 1);
			var startDay = first.getDay();
			var daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
			var daysInPrev = new Date(viewYear, viewMonth, 0).getDate();
			var todayDate = today();

			var cells = [];
			for (var i = startDay - 1; i >= 0; i--) {
				cells.push({
					day: daysInPrev - i,
					month: viewMonth - 1,
					year: viewMonth === 0 ? viewYear - 1 : viewYear,
					outside: true,
				});
			}
			for (var day = 1; day <= daysInMonth; day++) {
				cells.push({
					day: day,
					month: viewMonth,
					year: viewYear,
					outside: false,
				});
			}
			var remaining = 7 - (cells.length % 7);
			if (remaining < 7) {
				for (var i = 1; i <= remaining; i++) {
					cells.push({
						day: i,
						month: viewMonth + 1,
						year: viewMonth === 11 ? viewYear + 1 : viewYear,
						outside: true,
					});
				}
			}

			for (var r = 0; r < cells.length; r += 7) {
				html += "<tr>";
				for (var c = r; c < r + 7 && c < cells.length; c++) {
					var cell = cells[c];
					var cellDate = new Date(cell.year, cell.month, cell.day);
					var classes = "dpicker__day";
					if (cell.outside) {
						classes += " dpicker__day--outside";
					}
					if (isSameDay(cellDate, todayDate)) {
						classes += " dpicker__day--today";
					}
					if (selectedDate && isSameDay(cellDate, selectedDate)) {
						classes += " dpicker__day--selected";
					}
					if (
						(minDate && cellDate < clearTime(minDate)) ||
						(maxDate && cellDate > clearTime(maxDate))
					) {
						classes += " dpicker__day--disabled";
					}
					var iso = isoFromDate(cellDate);
					html +=
						'<td><button type="button" class="' +
						classes +
						'" data-dpicker-val="' +
						iso +
						'">' +
						cell.day +
						"</button></td>";
				}
				html += "</tr>";
			}
			html += "</tbody></table>";

			html += '<div class="dpicker__footer">';
			html +=
				'<button type="button" class="dpicker__today-btn" data-dpicker-today="true">' +
				(lang === "es" ? "Hoy" : "Today") +
				"</button>";
			html += "</div>";

			popup.innerHTML = html;

			popup.querySelector("[data-dpicker-prev]").addEventListener("click", function (e) {
				e.stopPropagation();
				viewMonth--;
				if (viewMonth < 0) {
					viewMonth = 11;
					viewYear--;
				}
				renderPopup();
			});

			popup.querySelector("[data-dpicker-next]").addEventListener("click", function (e) {
				e.stopPropagation();
				viewMonth++;
				if (viewMonth > 11) {
					viewMonth = 0;
					viewYear++;
				}
				renderPopup();
			});

			popup.querySelector("[data-dpicker-today]").addEventListener("click", function (e) {
				e.stopPropagation();
				selectDate(todayDate);
				close();
			});

			popup.querySelectorAll("[data-dpicker-val]").forEach(function (btn) {
				btn.addEventListener("click", function () {
					var iso = btn.getAttribute("data-dpicker-val");
					var date = parseISODate(iso);
					if (date) {
						if (
							(minDate && date < clearTime(minDate)) ||
							(maxDate && date > clearTime(maxDate))
						) {
							return;
						}
						selectDate(date);
						close();
					}
				});
			});
		}

		function selectDate(date) {
			selectedDate = date;
			hiddenInput.value = isoFromDate(date);
			updateDisplay();
			hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
		}

		btn.addEventListener("click", function (e) {
			e.stopPropagation();
			if (popup.classList.contains("dpicker__popup--open")) {
				close();
			} else {
				open();
			}
		});

		displayInput.addEventListener("focus", function () {
			if (!popup.classList.contains("dpicker__popup--open")) {
				open();
			}
		});

		document.addEventListener("click", function (e) {
			if (!container.contains(e.target)) {
				close();
			}
		});
	}

	function clearTime(date) {
		return new Date(date.getFullYear(), date.getMonth(), date.getDate());
	}

	function today() {
		var d = new Date();
		return new Date(d.getFullYear(), d.getMonth(), d.getDate());
	}

	function isSameDay(a, b) {
		return (
			a.getFullYear() === b.getFullYear() &&
			a.getMonth() === b.getMonth() &&
			a.getDate() === b.getDate()
		);
	}

	function parseISODate(str) {
		if (!str) return null;
		var parts = str.split("-");
		if (parts.length !== 3) return null;
		var year = parseInt(parts[0], 10);
		var month = parseInt(parts[1], 10) - 1;
		var day = parseInt(parts[2], 10);
		return new Date(year, month, day);
	}

	function isoFromDate(date) {
		var y = date.getFullYear();
		var m = String(date.getMonth() + 1).padStart(2, "0");
		var d = String(date.getDate()).padStart(2, "0");
		return y + "-" + m + "-" + d;
	}

	function formatDisplay(date, lang) {
		var m = String(date.getMonth() + 1).padStart(2, "0");
		var d = String(date.getDate()).padStart(2, "0");
		var y = date.getFullYear();
		return lang === "es" ? d + "/" + m + "/" + y : m + "/" + d + "/" + y;
	}

	document.addEventListener("DOMContentLoaded", function () {
		document.querySelectorAll("[data-dpicker='true']").forEach(initPicker);
	});
})();
