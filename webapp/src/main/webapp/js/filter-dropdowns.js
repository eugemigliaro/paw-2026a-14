(function () {
	'use strict';

	function closeAll() {
		document.querySelectorAll('.filter-dropdown.is-open').forEach(function (d) {
			d.classList.remove('is-open');
			var trigger = d.querySelector('.filter-dropdown__toggle');
			if (trigger) {
				trigger.setAttribute('aria-expanded', 'false');
			}
		});
	}

	function normalizePrice(value) {
		var parsed = parseFloat(value);
		return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
	}

	function normalizeDate(value) {
		if (!value) return null;
		var parts = value.split('-');
		if (parts.length !== 3) return null;
		return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
	}

	function validatePriceRange(form) {
		var minInput = form.querySelector('[data-price-from="true"]');
		var maxInput = form.querySelector('[data-price-to="true"]');
		if (!minInput || !maxInput) return true;

		var minValue = normalizePrice(minInput.value);
		var maxValue = normalizePrice(maxInput.value);
		var isValid = maxInput.value === '' || maxValue > minValue;
		maxInput.setCustomValidity(
			isValid
				? ''
				: maxInput.getAttribute('data-price-range-error') || ''
		);
		if (!isValid) {
			maxInput.reportValidity();
		}
		return isValid;
	}

	function clearDateErrors(form) {
		form.querySelectorAll('[data-dpicker-error]').forEach(function (el) {
			el.hidden = true;
			el.textContent = '';
		});
	}

	function validateDateRange(form) {
		clearDateErrors(form);

		var rangeError = form.getAttribute('data-date-range-error');
		if (!rangeError) {
			console.warn('Date range validation skipped: form is missing data-date-range-error attribute');
			return true;
		}

		var startInput = form.querySelector('[name="startDate"]');
		var endInput = form.querySelector('[name="endDate"]');
		if (!startInput || !endInput) return true;

		var startDate = normalizeDate(startInput.value);
		var endDate = normalizeDate(endInput.value);
		if (!startDate || !endDate) return true;

		if (endDate < startDate) {
			var endContainer = endInput.closest('[data-dpicker="true"]');
			if (endContainer) {
				var errorEl = endContainer.querySelector('[data-dpicker-error]');
				if (errorEl) {
					errorEl.textContent = rangeError;
					errorEl.hidden = false;
				}
			}
			return false;
		}
		return true;
	}

	document.addEventListener('click', function (e) {
		var closeButton = e.target.closest('.filter-dropdown__close');
		if (closeButton) {
			e.preventDefault();
			closeAll();
			return;
		}

		var toggle = e.target.closest('.filter-dropdown__toggle');
		if (toggle) {
			e.preventDefault();
			var dd = toggle.closest('.filter-dropdown');
			var wasOpen = dd.classList.contains('is-open');
			closeAll();
			if (!wasOpen) {
				dd.classList.add('is-open');
				toggle.setAttribute('aria-expanded', 'true');
			}
			return;
		}
		if (!e.target.closest('.filter-dropdown__panel')) {
			closeAll();
		}
	});

	document.addEventListener('input', function (e) {
		if (e.target.matches('[data-price-from="true"], [data-price-to="true"]')) {
			var form = e.target.closest('form');
			if (form) {
				var maxInput = form.querySelector('[data-price-to="true"]');
				if (maxInput) maxInput.setCustomValidity('');
			}
		}
	});

	document.addEventListener('change', function (e) {
		if (e.target.matches('[data-dpicker-iso="true"]')) {
			var form = e.target.closest('form');
			if (form) clearDateErrors(form);
		}
	});

	/* Remember which dropdown was open so we can reopen after page reload */
	document.addEventListener('click', function (e) {
		var item = e.target.closest('.filter-dropdown__item');
		if (item) {
			var dd = item.closest('.filter-dropdown');
			if (dd) {
				if (dd.getAttribute('data-close-on-select') === 'true') {
					sessionStorage.removeItem('pawOpenFilter');
					closeAll();
					return;
				}
				sessionStorage.setItem('pawOpenFilter', dd.getAttribute('data-filter-name') || '');
			}
		}
	});

	document.addEventListener('submit', function (e) {
		var panel = e.target.closest('.filter-dropdown__panel');
		if (panel) {
			if (!validatePriceRange(e.target)) {
				e.preventDefault();
				return;
			}
			if (!validateDateRange(e.target)) {
				e.preventDefault();
				return;
			}
			sessionStorage.removeItem('pawOpenFilter');
			closeAll();
		}
	});

	var saved = sessionStorage.getItem('pawOpenFilter');
	if (saved) {
		sessionStorage.removeItem('pawOpenFilter');
		var t = document.querySelector('.filter-dropdown[data-filter-name="' + saved + '"]');
		if (t) {
			t.classList.add('is-open');
			var trigger = t.querySelector('.filter-dropdown__toggle');
			if (trigger) {
				trigger.setAttribute('aria-expanded', 'true');
			}
		}
	}
})();
