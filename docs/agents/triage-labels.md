# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.

Category roles (from `/triage`) map to the repo's existing GitHub labels: `bug` and `enhancement`. An issue carries exactly one category role and one state role.

## Wayfinder labels

`/wayfinder` labels the map and every ticket it creates. The map is one issue; its tickets are child issues:

- `wayfinder:map` — the map issue
- `wayfinder:research` — research ticket (AFK)
- `wayfinder:prototype` — prototype ticket (HITL)
- `wayfinder:grilling` — grilling / discussion ticket (HITL)
- `wayfinder:task` — task ticket (HITL or AFK)

All five are created in this repo's tracker; `/wayfinder` applies them and nothing else.
