(function () {
	'use strict';

	function closeAll() {
		document.querySelectorAll('.filter-dropdown.is-open').forEach(function (d) {
			d.classList.remove('is-open');
		});
	}

	document.addEventListener('click', function (e) {
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
			var dd = panel.closest('.filter-dropdown');
			if (dd) sessionStorage.setItem('pawOpenFilter', dd.getAttribute('data-filter-name') || '');
		}
	});

	var saved = sessionStorage.getItem('pawOpenFilter');
	if (saved) {
		sessionStorage.removeItem('pawOpenFilter');
		var t = document.querySelector('.filter-dropdown[data-filter-name="' + saved + '"]');
		if (t) t.classList.add('is-open');
	}
})();
