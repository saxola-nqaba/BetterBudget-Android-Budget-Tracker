# BetterBudget — Android Budget Tracker

A personal budget tracking Android app built in Kotlin that helps users
log daily spending, set monthly goals, and visualise their budget
performance over time.

---

## Demo

> Demonstration video available on YouTube:
> https://www.youtube.com/@betterbudget-u9f

---

## Features

- Register and log in with email and password via Firebase Authentication
- Create, edit and delete custom spending categories with emoji icons
- Create subcategories nested under each category
- Log expenses with date, amount, description and optional receipt photo
- Set minimum and maximum monthly spending goals per category and subcategory
- Filter and sort expenses by date range
- View category breakdown with subcategory totals and progress bars
- Bar graph showing total spent per category over a user-selected date range
  with minimum and maximum goal lines overlaid
- Colour-coded progress indicators showing how close spending is to each
  goal threshold at a glance
- Snooze Goals — temporarily suppress budget warnings for a category for
  the remainder of the month, with snooze state stored in Firebase and
  auto-reset after the period ends
- Recurring expense automation for predictable costs such as subscriptions
  and rent, with rules triggering on app open if the scheduled date has passed

---

## Tech Stack

- Language: Kotlin
- Architecture: MVVM
- Database: Firebase Realtime Database
- Authentication: Firebase Authentication
- Async: Kotlin Coroutines and Flow
- UI: XML Layouts and Fragments
- Charts: MPAndroidChart
- Min SDK: 26 / Target SDK: 36

---

## What I Learned

Leading the Firebase migration for the Categories layer taught me how
architectural decisions at the data layer ripple through the entire app.
Categories link to expenses, goals, and subcategories, so getting the
Firebase data structure right early prevented cascading issues later.
Building the Snooze Goals feature from scratch — including designing the
state model, storing it in Firebase, and wiring the auto-reset logic —
was the most end-to-end feature I owned independently, and it gave me a
clear picture of how custom business logic sits between the UI and the
database layer in an MVVM architecture.

---

## How to Run

```bash
git clone https://github.com/saxola-nqaba/BetterBudget-Android-Budget-Tracker.git
cd BetterBudget-Android-Budget-Tracker
```

1. Open the project in Android Studio
2. Sync Gradle
3. Add your `google-services.json` file to the `/app` directory
4. Connect a physical Android device running API 26 or higher
5. Run the app

---

## Project Context

Built as part of Advanced Android Development (PROG7313) at Varsity College
(IIE), Midrand — group project, team of four.

My specific contributions:
- Led the Firebase migration for the Categories data layer
- Owned the Expenses section including XML layouts, Firebase integration,
  filtering and sorting
- Designed and built the Snooze Goals feature end to end
- Contributed to the Goals section UI and functionality

---

## Future Improvements

- Migrate automation rules to WorkManager for reliable background scheduling
- Add CSV or PDF export for monthly expense reports
- Implement biometric login as an alternative to email and password
- Dark mode support