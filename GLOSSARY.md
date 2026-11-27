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
_Avoid_: User, athlete

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

**Admin**:
A Member who manages all Clubs and Members, not just those of their own
Clubs.

**Instructor**:
A Member who manages the Members of their own Clubs. An Instructor is an
Instructor in every Club they belong to.
_Avoid_: Trainer, coach
