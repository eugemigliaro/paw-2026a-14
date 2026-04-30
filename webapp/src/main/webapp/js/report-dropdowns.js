document.addEventListener('DOMContentLoaded', function() {
    const dropdown = document.getElementById('reason-dropdown');
    const toggle = document.getElementById('reason-toggle');
    const label = document.getElementById('selected-reason-label');
    const select = document.getElementById('report-reason');
    const items = dropdown.querySelectorAll('.report-dropdown__item');

    toggle.addEventListener('click', function() {
        dropdown.classList.toggle('is-open');
    });

    items.forEach(item => {
        item.addEventListener('click', function() {
            const value = this.dataset.value;
            const text = this.innerText;

            // Update selection
            select.value = value;
            label.innerText = text;

            // Update active state
            items.forEach(i => i.classList.remove('report-dropdown__item--active'));
            this.classList.add('report-dropdown__item--active');

            // Close dropdown
            dropdown.classList.remove('is-open');
        });
    });

    // Close on outside click
    document.addEventListener('click', function(event) {
        if (!dropdown.contains(event.target)) {
            dropdown.classList.remove('is-open');
        }
    });
});
