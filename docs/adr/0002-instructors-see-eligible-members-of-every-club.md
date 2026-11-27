# Instructors see the Eligible Members of every Club

An Instructor otherwise sees only the Members of their own Clubs. The
Eligible list (`GET /api/v1/eligible?date=`) is an exception: it shows the
Members of every Club who are Eligible for a Dan or Poom on the given date.
Black-belt Graduations are held for all of Wandy Wha Rang, and the Instructors
who plan them need every Club's candidates. The exception is kept narrow. The
list carries only name, Clubs, current Belt, latest Graduation date, next Belt
and the date the Member became Eligible. It carries no contact details and no
date of birth, and the Member record itself stays limited to the Member's own
Clubs.

## Considered Options

- **Compute the list in the frontend from `GET /members`**: needs no new
  endpoint, but an Instructor only gets their own Clubs' Members.
- **Widen `GET /members` for Instructors**: would hand them every Member's
  address, email and phone.
