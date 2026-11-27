# A Member's Role lives only in Keycloak

A Member's Role (Plain Member, Instructor or Admin) is held only as Keycloak
realm roles: everyone has `member`, an Instructor also has `instructor` and an
Admin also has `admin`. member-service reads and writes those roles through
the Keycloak admin client it already uses to create Members. It keeps no copy
of the Role in Postgres. The token is what grants access, so Keycloak has to
hold the Role anyway, and a second copy could drift from it, for example when
someone changes a role in the Keycloak console.

## Considered Options

- **A role column on the member table, synced to Keycloak**: the Role could
  be listed and filtered in SQL, but every change would have to be written
  in two places and kept in step, and changes made in the Keycloak console
  would be missed.

## Consequences

- Reading a Member's Role costs a Keycloak call. That's why it is filled in
  only for Admins, on a single Member, and not on the Member list.
- A changed Role takes effect at the person's next token refresh, not at
  once.
- The member-service service account needs enough realm-management rights
  to look up and map realm roles.
