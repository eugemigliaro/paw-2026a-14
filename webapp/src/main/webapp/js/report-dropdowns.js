document.addEventListener('DOMContentLoaded', function() {
    const dropdowns = document.querySelectorAll('.report-dropdown');

    dropdowns.forEach(dropdown => {
        const toggle = dropdown.querySelector('.report-dropdown__toggle');
        const label = dropdown.querySelector('[id^="selected-"][id$="-label"]') || dropdown.querySelector('.report-dropdown__label');
        const select = dropdown.querySelector('select');
        const items = dropdown.querySelectorAll('.report-dropdown__item');

        if (!toggle || !items.length || !select) return;

        toggle.addEventListener('click', function(e) {
            e.stopPropagation();
            // Close other dropdowns first
            dropdowns.forEach(other => {
                if (other !== dropdown) other.classList.remove('is-open');
            });
            dropdown.classList.toggle('is-open');
        });

        items.forEach(item => {
            item.addEventListener('click', function(e) {
                e.stopPropagation();
                const value = this.dataset.value;
                const text = this.innerText;

                // Update selection
                select.value = value;
                if (label) label.innerText = text;

                // Update active state
                items.forEach(i => i.classList.remove('report-dropdown__item--active'));
                this.classList.add('report-dropdown__item--active');

                // Close dropdown
                dropdown.classList.remove('is-open');

                // Trigger change event on select
                select.dispatchEvent(new Event('change'));
            });
        });
    });

    // Close on outside click
    document.addEventListener('click', function(event) {
        dropdowns.forEach(dropdown => {
            if (!dropdown.contains(event.target)) {
                dropdown.classList.remove('is-open');
            }
        });
    });
});
