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

    function parseLocalDateTime(dateValue, timeValue) {
        if (!dateValue || !timeValue) {
            return null;
        }

        var dateParts = dateValue.split("-").map(Number);
        var timeParts = timeValue.split(":").map(Number);

        if (dateParts.length !== 3 || timeParts.length < 2) {
            return null;
        }

        return new Date(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1], 0, 0);
    }

    function pad(value) {
        return String(value).padStart(2, "0");
    }

    function setFieldError(field, text) {
        if (!field) {
            return;
        }

        var error = field.querySelector("[data-field-error='true']");
        if (!error) {
            return;
        }

        error.textContent = text || "";
    }

    function initScheduleEditor() {
        var editor = document.querySelector("[data-bracket-schedule-editor='true']");
        if (!editor) {
            return;
        }

        var summaryList = editor.querySelector("[data-validation-summary='true']");
        var summaryEmpty = editor.querySelector("[data-validation-empty='true']");
        var summaryTitle = editor.getAttribute("data-summary-title") || "";
        var nowLabel = editor.getAttribute("data-error-before-now") || "";
        var rangeLabel = editor.getAttribute("data-error-invalid-range") || "";
        var roundLabel = editor.getAttribute("data-error-round-order") || "";
        var rounds = editor.querySelectorAll("[data-round-number]");

        function updateSummary(messages) {
            if (!summaryList || !summaryEmpty) {
                return;
            }

            summaryList.innerHTML = "";
            if (!messages.length) {
                summaryEmpty.hidden = false;
                summaryList.hidden = true;
                return;
            }

            summaryEmpty.hidden = true;
            summaryList.hidden = false;
            messages.forEach(function (message) {
                var item = document.createElement("li");
                item.textContent = message;
                summaryList.appendChild(item);
            });
        }

        function validateEditor() {
            var matchCards = editor.querySelectorAll("[data-match-schedule='true']");
            var now = new Date();
            var messages = [];
            var roundsInfo = [];

            matchCards.forEach(function (card) {
                var matchLabel = card.getAttribute("data-match-label") || "Match";
                var roundLabelText = card.getAttribute("data-round-label") || "Round";
                var startDateInput = card.querySelector("[data-start-date='true']");
                var startTimeInput = card.querySelector("[data-start-time='true']");
                var endDateInput = card.querySelector("[data-end-date='true']");
                var endTimeInput = card.querySelector("[data-end-time='true']");
                var startField = card.querySelector("[data-start-field='true']");
                var endField = card.querySelector("[data-end-field='true']");
                var start = parseLocalDateTime(startDateInput && startDateInput.value, startTimeInput && startTimeInput.value);
                var end = parseLocalDateTime(endDateInput && endDateInput.value, endTimeInput && endTimeInput.value);

                setFieldError(startField, "");
                setFieldError(endField, "");

                if (start && start.getTime() < now.getTime()) {
                    var beforeNowMessage = matchLabel + " - " + nowLabel;
                    messages.push(beforeNowMessage);
                    setFieldError(startField, nowLabel);
                }

                if (start && end && end.getTime() <= start.getTime()) {
                    var rangeMessage = matchLabel + " - " + rangeLabel;
                    messages.push(rangeMessage);
                    setFieldError(endField, rangeLabel);
                }

                if (start && end) {
                    roundsInfo.push({
                        roundNumber: Number(card.getAttribute("data-round-number")),
                        roundLabel: roundLabelText,
                        matchLabel: matchLabel,
                        startsAt: start,
                        endsAt: end
                    });
                }
            });

            var latestByRound = {};
            roundsInfo.forEach(function (info) {
                if (!latestByRound[info.roundNumber] || info.endsAt.getTime() > latestByRound[info.roundNumber].getTime()) {
                    latestByRound[info.roundNumber] = info.endsAt;
                }
            });

            roundsInfo.forEach(function (info) {
                var previousRoundEnd = latestByRound[info.roundNumber - 1];
                if (previousRoundEnd && info.startsAt.getTime() < previousRoundEnd.getTime()) {
                    messages.push(info.matchLabel + " - " + roundLabel);
                }
            });

            rounds.forEach(function (roundSection) {
                var helper = roundSection.querySelector("[data-round-helper='true']");
                if (!helper) {
                    return;
                }
                helper.classList.toggle("is-warning", messages.length > 0);
            });

            var deduped = Array.from(new Set(messages));
            updateSummary(deduped.map(function (message, index) {
                return summaryTitle + " " + (index + 1) + ": " + message;
            }));
        }

        editor.querySelectorAll("[data-match-schedule='true']").forEach(function (card) {
            var startDateInput = card.querySelector("[data-start-date='true']");
            var startTimeInput = card.querySelector("[data-start-time='true']");
            var endDateInput = card.querySelector("[data-end-date='true']");
            var endTimeInput = card.querySelector("[data-end-time='true']");

            [startDateInput, startTimeInput].forEach(function (input) {
                if (!input) {
                    return;
                }
                input.addEventListener("change", function () {
                    validateEditor();
                });
            });

            [endDateInput, endTimeInput].forEach(function (input) {
                if (!input) {
                    return;
                }
                input.addEventListener("change", function () {
                    validateEditor();
                });
            });
        });
        validateEditor();
    }

    function initBracketSetup() {
        var select = document.getElementById("pairing-strategy");
        var updateButton = document.getElementById("update-strategy-button");
        var generateButton = document.getElementById("generate-bracket-button");

        if (select && updateButton && generateButton) {
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

        initScheduleEditor();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initBracketSetup);
        return;
    }

    initBracketSetup();
})();
