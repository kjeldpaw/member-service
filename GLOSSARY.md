# Member Service

Keeps the membership records of Wandy Wha Rang: its clubs, members, belts
and graduations.

## Language

**Wandy Wha Rang**:
The Taekwondo organisation that the Clubs belong to. Written `wandywharang`
where spaces are not allowed.
_Avoid_: Wandyhwarang, WandyHwarang

**Club**:
A Taekwondo club within Wandy Wha Rang, such as Randers Taekwondo Klub.
_Avoid_: Dojang, department

**Member**:
A person registered in Wandy Wha Rang who belongs to one or more Clubs.
Not the same as the `member` login role, which any person who can log in has.
A Member may have a login, which is their email. A Member without a login,
such as a child, has no email and is looked after by their Guardians.
_Avoid_: User, athlete, username, account, submember

**Guardian**:
A Member who looks after another Member's record, such as a parent of a
child who trains. A Member can have several Guardians and a Guardian can
look after several Members. A Guardian sees and edits the Profile of each
Member they look after as if it were their own, and can give a Member
without a login their own login. A Guardian stays a Guardian, also once the
Member has their own login, until an Instructor or Admin ends it. Only an
Instructor or Admin makes a Member someone's Guardian, and the two need not
share a Club.
_Avoid_: Parent, family, household, submember

**Profile**:
The Member record of the person who is logged in, as they see it
themselves. Everyone who can log in should be a Member; someone who logs in
without a Member record creates their Profile first.
_Avoid_: Account, user data

**Reference**:
A Member's number in an outside register, either Wandy Wha Rang's or
Kukkiwon's.
_Avoid_: Registration, ID

## Grading

**Belt**:
A Mon, Kup, Poom or Dan grade a Member can hold. Each Belt has a minimum
time a Member holds it before grading to the next.
_Avoid_: Grade, rank

**Poom**:
The black-belt grade, 1. to 4. Poom, for a Member under 15 on the day of the
Graduation; from 15 the next black belt is a Dan. A Member whose age is not
known goes for a Dan.
_Avoid_: Junior Dan

**Graduation**:
A Member's promotion to a Belt on a given date, examined by one or more
Examiners. A Member's Graduations, latest first, form their Graduation
history.
_Avoid_: Grading, exam, promotion

**Eligible**:
A Member is Eligible for their next Belt on a given date when, by one
calendar month after that date, they will have held their current Belt for
at least its minimum time.
_Avoid_: Qualified, ready, candidate

**Examiner**:
A Member who examined a Graduation.
_Avoid_: Grader, graduatedBy

## Roles

**Role**:
What a Member may do: Plain Member, Instructor or Admin. A Member with a
login has exactly one Role, and each Role can do everything the Roles below
it can.
A Member without a login has no Role; they become a Plain Member when they
get one.
Only an Admin gives a Member a Role, and never to themselves.
_Avoid_: Permission, access level

**Plain Member**:
A Member whose Role is neither Instructor nor Admin. They see and edit only
their own Profile and the Profiles of the Members they are Guardian of.
_Avoid_: Member (for the Role), user

**Admin**:
A Member who manages all Clubs and Members, not just those of their own
Clubs.

**Instructor**:
A Member who manages the Members of their own Clubs. An Instructor is an
Instructor in every Club they belong to.
_Avoid_: Trainer, coach
