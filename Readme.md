Quiz Leaderboard System — SRM Internship Assignment


A simple single-file Java application that polls a quiz API 10 times, removes duplicate events, calculates total scores per participant, and submits the final leaderboard.
Prerequisites

Java 8 or above

Project Structure
QuizLeaderboard/
├── Main.java     # All code is here
└── README.md
Setup & Run
Step 1: Add your Registration Number
Open Main.java and update line 7:


javastatic final String REG_NO = "YOUR_REG_NO";


Replace YOUR_REG_NO with your actual registration number, e.g. "RA2311050010025".
Step 2: Compile
bashjavac Main.java
Step 3: Run
bashjava Main
The program will take about 50 seconds to complete (10 polls × 5 second delay).
How It Works

Polls GET /quiz/messages?regNo=<regNo>&poll=0 through poll=9
Waits 5 seconds between each poll (as required)
For every event received, checks if roundId + participant was already seen — if yes, skips it as a duplicate
Accumulates the total score per participant
Sorts the leaderboard by total score (highest first)
Submits the leaderboard once to POST /quiz/submit
Prints the server response — should show "isCorrect": true

Duplicate Handling
The API may return the same event data across multiple polls. This is handled using a HashSet that tracks each unique roundId_participant combination. If the same combination appears again, it is ignored.



Poll 1 → Alice R1 +10  ✅ Added
Poll 4 → Alice R1 +10  ❌ Duplicate, skipped
Final score for Alice = 10 ✓