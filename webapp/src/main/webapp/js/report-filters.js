(function () {
	var form = document.getElementById('report-filter-form');
	if (!form) {
		return;
	}

	form.querySelectorAll('input[type="checkbox"]').forEach(function (cb) {
		cb.addEventListener('change', function () {
			form.submit();
		});
	});
})();
