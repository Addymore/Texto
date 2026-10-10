# QUIK upstream maintenance

Before each Texto change/release, fetch `upstream/master` from https://github.com/quik-sms/quik and review the new source commits together with https://github.com/quik-sms/quik/actions. Apply relevant reviewed fixes to Texto, retain authorship/source references, and run the local regression suites. A successful housekeeping workflow is not evidence of an Android build passing. Do not use unapproved pull-request artifacts as validated source.

## Checkpoint: 10 October 2026

- Reviewed upstream master `02542049207bab8e467a90cc0e50d3febe2b6a8d`.
- Incorporated its unused billing-version removal into Texto's root build file.
- The other new commits remove old QKSMS+ UI and strings. They overlap Texto's modified backup/privacy/settings screens and require a separate migration; no new message-selection fix is present in these commits.
- Runs 37797998238 and 37797715680 require approval. Run 36799149810 succeeded for Auto-Update MMS configs; it is not an APK build certification.
- Keep Texto's number-based privacy receive paths, signing identity, themes and existing tests when integrating further changes.
