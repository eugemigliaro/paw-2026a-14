(function () {
  function initHostInviteTrigger() {
    var triggers = document.querySelectorAll('[data-host-invite-trigger="true"]');
    var panel = document.getElementById('pending-invitations');
    var emailInput = document.getElementById('host-invite-email');

    if (!triggers.length || !panel) {
      return;
    }

    triggers.forEach(function (trigger) {
      trigger.addEventListener('click', function () {
        panel.open = true;
        window.setTimeout(function () {
          panel.scrollIntoView({ block: 'start', behavior: 'smooth' });
          if (emailInput) {
            emailInput.focus({ preventScroll: true });
          }
        }, 0);
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHostInviteTrigger);
    return;
  }

  initHostInviteTrigger();
})();
