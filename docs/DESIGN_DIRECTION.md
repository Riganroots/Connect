# Connect: a social activity app

The first screen should help someone find a real plan and understand how to join. It should feel useful even when only a few plans exist.

## Shared rules

- A compact wordmark/location header and persistent Discover, Create and My plans navigation.
- A 12/14/16/20/24 type scale, 20dp screen gutters and restrained 12–18dp corners.
- Green identifies actionable/selected items. White and neutral surfaces carry content.
- Photography belongs in category discovery and the detail cover. Repeated stock images must not dominate a list of real plans.
- Scan title, date and meeting point in the feed. Decide to join on the detail page. Keep cost, host and group context visible.
- No invented activity ratings, participant portraits, urgency labels or map distances. Category imagery stays labelled and attributed.

## Main screens

Discovery uses compact category imagery and readable plan summaries. A plan card opens a full detail page before joining. My plans brings joined, hosted and saved activities together with a small editable member summary. Account actions remain accessible below. The create screen uses section spacing rather than nested cards, with native calendar and time controls. Sign-in uses the same wordmark, type scale and short explanation of the social purpose.

The preview-only simulated identity-verification wizard has been removed. It never performed real verification and is unrelated to activity coordination. Production auth and cloud rules are unchanged.

## Review

Native CI previews include the full discovery shell, My plans, sign-in, activity details, empty feed and 1.3x text. Fixtures are test-only. Review on a real phone is still needed, including keyboard, system insets, and the live two-account flow.
