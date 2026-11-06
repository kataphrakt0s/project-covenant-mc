# Networking and Authority

Gameplay-critical state and mutations are server-authoritative. This includes character XP/levels, skill points and ranks, profession XP/levels/points/unlocks, contributor XP attribution, dimensional progression gates, recipe eligibility, respec operations, class resources, stats, equipment, and ability execution.

Clients may request actions, but the server validates the player's current dimension, progression caps, activity eligibility, prerequisites, point budget, eight-skill allocation limit, costs, and item requirements. Never trust client-supplied progression values or combat/activity outcomes.

Synchronize the progression and unlock data needed for client presentation without making the client authoritative.

Related: [[Player Profiles]], [[Progression Systems]], [[Experience and Reward Systems]], [[Dimensional Progression Gates]], [[Combat Skill Progression]], [[Profession Progression]], [[Combat]], [[Class Resources]], [[Technical Architecture]].
