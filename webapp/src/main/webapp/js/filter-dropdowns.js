(function () {
	'use strict';

	function closeAll() {
		document.querySelectorAll('.filter-dropdown.is-open').forEach(function (d) {
			d.classList.remove('is-open');
		});
	}

	function normalizePrice(value) {
		var parsed = parseFloat(value);
		return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
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
				: maxInput.getAttribute('data-price-range-error') || 'To must be greater than from.'
		);
		if (!isValid) {
			maxInput.reportValidity();
		}
		return isValid;
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
			if (!wasOpen) dd.classList.add('is-open');
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

	/* Remember which dropdown was open so we can reopen after page reload */
	document.addEventListener('click', function (e) {
		var item = e.target.closest('.filter-dropdown__item');
		if (item) {
			var dd = item.closest('.filter-dropdown');
			if (dd) sessionStorage.setItem('pawOpenFilter', dd.getAttribute('data-filter-name') || '');
		}
	});

	document.addEventListener('submit', function (e) {
		var panel = e.target.closest('.filter-dropdown__panel');
		if (panel) {
			if (!validatePriceRange(e.target)) {
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
		if (t) t.classList.add('is-open');
	}
})();
