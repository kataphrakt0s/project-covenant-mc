# Networking and Authority

Gameplay-critical state and mutations are server-authoritative. This includes character XP/levels, skill points and ranks, profession XP/levels/points/unlocks, contributor XP attribution, dimensional progression gates, recipe eligibility, respec operations, class resources, stats, equipment, and ability execution.

Clients may request actions, but the server validates the relevant action/source
dimension, progression caps, activity eligibility, prerequisites, point budget,
eight-skill allocation limit, costs, and item requirements. Combat XP uses the
enemy's death dimension for its gate, including when a contributor has moved or
respawned elsewhere. Never trust client-supplied progression values or
combat/activity outcomes.

Synchronize the progression and unlock data needed for client presentation without making the client authoritative.

Planned skill-tree edits are pending until Apply. Node clicks stage additions
or undo pending additions; they cannot refund committed ranks or grant skill
effects. Apply submits one bounded intent request, and the server validates the
complete allocation against its current authoritative state. Commit valid
investments together only when the player is out of combat under the shared
seven-second timer, or reject without partial spending. Repeated or stale
requests cannot purchase the same draft twice. See [[Combat Skill Progression]].

Related: [[Player Profiles]], [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Combat Skill Progression]], [[Profession Progression]], [[Combat]], [[Class Resources]], [[Technical Architecture]].
