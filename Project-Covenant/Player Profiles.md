# Player Profiles

Persistent per-player RPG data: initialization state, level/XP, class/specialization, resource identity/current/max. Later: attributes, talents, unlocks.

Persist across logout, death, respawn, and restarts. Handle cloning/respawn, synchronize relevant client data, and keep mutation server-authoritative. Do not store equipped item modifiers as permanent profile data.

Related: [[Character Progression]], [[Classes and Specializations]], [[Class Resources]], [[Networking and Authority]].
