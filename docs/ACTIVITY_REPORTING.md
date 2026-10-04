# Activity reporting

Real-account participants can open their activity chat and select **Report** in
its header. Choose a reason and optionally add up to 1,000 characters of detail.
The host cannot report their own activity. Preview content has no report button.
Confirmation appears only after Firestore acknowledges submission. After 20
seconds without acknowledgement the dialog explains that the queued write may
still complete when connectivity returns; retrying can submit a duplicate.

Reports store reporter UID, activity ID, host UID, reason, detail, and server
timestamp. Rules validate membership, identities and lengths. Clients cannot
read, edit or delete reports. A report does not automatically penalize a host.

Reporting does not block the host or leave the activity. Blocking and reporting
individual messages are upcoming tasks. Moderation currently requires a trusted
operator to review reports in Firebase Console. No moderation dashboard,
moderator notification, or response SLA is implemented. Assign a moderation
owner before launch. Deploy tested rules after comparing live rules.

## Verification

- Emulator tests cover valid submissions, private access, spoofed identities,
  extra moderator fields, invalid reasons, oversize detail and self/outsider reports.
- Android CI must compile the new repository and chat action before merging.
- Submit from a joined participant on a real device and verify the record with
  trusted admin access; check the host cannot retrieve it through the app.
- Check small-screen dialog layout, cancel, duplicate-tap prevention, offline
  timeout and submission error/retry.
