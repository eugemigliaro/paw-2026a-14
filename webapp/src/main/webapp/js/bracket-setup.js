(function () {
    function isDirty(select) {
        return select.value !== (select.dataset.initialValue || "");
    }

    function syncGenerateButtonState(select, button, tooltip) {
        if (!select || !button) {
            return;
        }

        var dirty = isDirty(select);
        button.disabled = dirty;
        button.setAttribute("aria-disabled", dirty ? "true" : "false");
        button.classList.toggle("is-disabled", dirty);

        if (tooltip) {
            tooltip.classList.toggle("report-tooltip", dirty);
            tooltip.setAttribute("aria-disabled", dirty ? "true" : "false");
            if (dirty) {
                tooltip.setAttribute("tabindex", "0");
            } else {
                tooltip.removeAttribute("tabindex");
            }
        }
    }

    function syncUpdateButtonState(select, button) {
        if (!select || !button) {
            return;
        }

        var dirty = isDirty(select);
        button.classList.toggle("tournament-bracket-actions__update-button--hidden", !dirty);
        button.disabled = false;
        button.setAttribute("aria-disabled", "false");
    }

    function initBracketSetup() {
        var select = document.getElementById("pairing-strategy");
        var updateButton = document.getElementById("update-strategy-button");
        var generateButton = document.getElementById("generate-bracket-button");
        var generateTooltip = document.getElementById("generate-bracket-button-tooltip");

        if (select && updateButton && generateButton) {
            syncUpdateButtonState(select, updateButton);
            syncGenerateButtonState(select, generateButton, generateTooltip);

            select.addEventListener("change", function () {
                syncUpdateButtonState(select, updateButton);
                syncGenerateButtonState(select, generateButton, generateTooltip);
            });

            select.addEventListener("input", function () {
                syncUpdateButtonState(select, updateButton);
                syncGenerateButtonState(select, generateButton, generateTooltip);
            });
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initBracketSetup);
        return;
    }

    initBracketSetup();
})();
