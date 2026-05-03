document.getElementById('report-filter-form')
    .querySelectorAll('input[type="checkbox"]')
    .forEach(function (cb) {
        cb.addEventListener('change', function () {
            document.getElementById('report-filter-form').submit();
        });
    });
