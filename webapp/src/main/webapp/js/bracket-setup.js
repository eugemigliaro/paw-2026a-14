(function () {
    function isDirty(select) {
        return select.value !== (select.dataset.initialValue || "");
    }

    function syncGenerateButtonState(select, button) {
        if (!select || !button) {
            return;
        }

        var dirty = isDirty(select);
        button.disabled = dirty;
        button.setAttribute("aria-disabled", dirty ? "true" : "false");
        button.classList.toggle("is-disabled", dirty);
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

        if (!select || !updateButton || !generateButton) {
            return;
        }

        syncUpdateButtonState(select, updateButton);
        syncGenerateButtonState(select, generateButton);

        select.addEventListener("change", function () {
            syncUpdateButtonState(select, updateButton);
            syncGenerateButtonState(select, generateButton);
        });

        select.addEventListener("input", function () {
            syncUpdateButtonState(select, updateButton);
            syncGenerateButtonState(select, generateButton);
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initBracketSetup);
        return;
    }

    initBracketSetup();
})();
