# Study Ta Bai (Java Swing)

AI-supported, gamified supplemental learning and teaching platform. Desktop app, Java 17+, Swing.
Teachers need an account. Students never do: they join with a join code and a nickname.

## 1. Architecture overview

| Area | Decision |
|---|---|
| UI | Swing (Metal look & feel re-skinned as a sunny Filipino province: light cream/leaf-green/wood-brown/sky-blue palette, plus a high-contrast mode). The animated scenery (sky and clouds, sea with a bangka, hills, rice terraces, farmers in salakots, bahay kubo, sari-sari store, palms, banderitas, a carabao cart and a jeepney) is all painted with Java2D, so there are no image files or extra libraries. Bundled fonts in `resources/fonts` (SIL OFL): Crimson Text Bold Italic for titles and Fira Sans ExtraBold for buttons and tabs |
| Persistence | Java **serialization**: `DataStore<T>` writes `data/*.ser` atomically (temp file, then move) |
| Deserialization safety | Every read goes through `SafeDeserializer` (allow-list of our packages + `java.base`, depth/size limits) |
| Live play | Teacher app hosts a `ServerSocket` on the LAN. Messages are serialized `Messages.*` objects over `ObjectOutputStream` |
| AI | `AiClient` interface -> `AnthropicClient` (plain `java.net.http`, key from `ANTHROPIC_API_KEY`, optional `ANTHROPIC_MODEL`) |
| Parsing | TXT/DOCX with the JDK; PDF with Apache PDFBox (optional, loaded by reflection) |
| Export | CSV (injection-safe) and PDF (built-in minimal writer, no library) |
| Security | PBKDF2 password hashing + lockout, input validation, server-side scoring and timing, rate limits (AI, joins, reactions), prompt-injection isolation |
| Privacy | Students send only a nickname. Uploaded files are not kept, only their extracted text |

### The four pillars
| Pillar | Where |
|---|---|
| Abstraction | `Question`, `Activity`, `User`, `Session`, `AiTask` (abstract); `AiClient`, `Exporter`, `Reportable`, `ScoringStrategy` (interfaces) |
| Inheritance | `MultipleChoice/TrueFalse/ShortAnswerQuestion` extend `Question`; `Quiz`, `FlashcardSet` extend `Activity`; `LiveSession`, `SelfPacedSession` extend `Session`; `QuizGenTask`, `TranslateTask`... extend `AiTask`/`TextTask`; `Teacher` extends `User` |
| Polymorphism | `isCorrect()`, `toStudentView()`, `canAnswer()`, `effectiveTimeMs()`, `AiTask.parse()`, `Exporter.export()`, `ScoringStrategy.points()` |
| Encapsulation | Private fields, validating setters, read-only list views, immutable DTOs (`Answer`, `Option`, `QuestionView`, messages) |

## 2. Data model (replaces a DB schema)
```
User (abstract) -> Teacher
Teacher owns: Document, Activity, AiResource, Session
Activity (abstract) -> Quiz (Questions) | FlashcardSet (Flashcards)
Question (abstract) -> MultipleChoiceQuestion(Option 2..6) | TrueFalseQuestion | ShortAnswerQuestion
Question -> standard codes (Standard: framework + code + description)
Session (abstract) -> LiveSession | SelfPacedSession
Session has: Participant (nickname, hashed rejoin token, optional Accommodation), AnswerRecord, session Accommodation
Accommodation: text-to-speech, time multiplier, high contrast, font scale
```

## 3. Folder structure
```
studytabai-java/
  pom.xml  run.sh  run.bat  test.sh  README.md
  data/            serialized files, created at runtime
  lib/             optional: PDFBox jars for non-Maven runs
  src/main/resources/prompts/   quiz, flashcards, generate, differentiate, translate, reading_level (.txt)
  src/main/java/ph/studytabai/
    Main, AppContext
    model/    User, Teacher, Question + 3 types, Option, Quiz, FlashcardSet, Flashcard, Document, Standard,
              Participant, Accommodation, Answer, QuestionView, AiResource, HistoryEntry
    session/  Session, LiveSession, SelfPacedSession, ScoringMode/Strategy, AnswerRecord, LeaderboardEntry
    storage/  DataStore, SafeDeserializer
    net/      GameServer, GameClient, Messages
    ai/       AiClient, AnthropicClient, AiTask, QuizGenTask, FlashcardGenTask, TextTask + 4 tasks, PromptLibrary, AiRateLimiter
    service/  AuthService, PasswordHasher, DocumentParser, TextToSpeech, MusicPlayer, MemeBank, StandardsSeed
    report/   ReportService, Reportable, Table, CsvExporter, PdfExporter, SimplePdf
    util/     Json
    ui/       MainFrame, Theme, Ui, Fonts, Palette, Art, ProvinceScene, WoodCard, WoodButtonUI, WoodTabbedPaneUI, LeafScrollBarUI, LoginPanel, TeacherHome, LessonsPanel, ActivitiesPanel, QuizEditorDialog,
              QuestionDialog, FlashcardEditorDialog, StudyTaBaiPanel, SessionsPanel, ReportsPanel, StandardsPanel, StudentPanel
  src/test/java/   ModelTests, LogicTests, ui/UiSmokeTest
```

## 4. Features -> where
- **Create & customize:** Lessons tab (upload/extract/edit), Quizzes & flashcards tab (manual editor, reorder, timers, points, images, standard tags, AI generation)
- **Study Ta Bai:** generate resource, differentiate (easier/on-level/advanced), translate, reading level; results are editable and saved only on request
- **Run sessions:** live (lobby, question flow, leaderboard, emoji reactions + fun captions, optional music, start/pause/resume/skip/end) or self-paced with a due date; flashcard sets as homework
- **Track data:** class report (average, completion, hardest questions, mastery by standard), student report (per-question, time, progress over time), CSV/PDF export
- **Join easily:** host + port + code + nickname; optional local "save my progress" history
- **Engage & learn:** speed or practice scoring, flashcards, instant feedback; read-aloud, extended timers, high contrast, font size, keyboard shortcuts (1-6, Enter, Space, arrows); teacher applies accommodations per session or per student

## 5. AI prompt templates
In `src/main/resources/prompts/`: `quiz.txt`, `flashcards.txt`, `generate.txt`, `differentiate.txt`, `translate.txt`, `reading_level.txt`.
Lesson text is sent in the user message inside `<lesson>` tags and every system prompt says to treat it as data, which reduces prompt-injection from uploaded documents.
Model output is parsed defensively: invalid questions are skipped, never trusted.

## 6. Setup and run
1. Install a JDK 17+ (and optionally Maven).
2. For AI features set the key before starting: `export ANTHROPIC_API_KEY=...` (Windows: `setx ANTHROPIC_API_KEY ...`, then reopen the terminal).
3. Run: `mvn compile exec:java`   **or**   `./run.sh` (Windows: `run.bat`).
4. For PDF lessons without Maven, place `pdfbox-3.0.x.jar` and its dependencies (`fontbox`, `pdfbox-io`, `commons-logging`) in `lib/`.
5. Linux read-aloud needs `espeak-ng` (or `espeak`/`spd-say`); Windows and macOS use built-in speech.
6. Use `-Dstb.data=/some/folder` to change where data is saved.

To try it on one computer: sign up as a teacher, upload a lesson, generate or write a quiz, host a session, then start a second copy of the app as a student and join with `127.0.0.1`, the port and the code.

## 7. Automated tests
`./test.sh` compiles and runs `ModelTests`, `LogicTests` (auth, parser, JSON, AI tasks with a fake model, session rules, sockets, reports, exports, deserialization filter) and `UiSmokeTest` (headless: builds every screen and drives the student screen against a real server).

## 8. Manual testing checklist
- [ ] Register, log out, log in; 5 wrong passwords lock the account for a minute
- [ ] Upload TXT, DOCX and PDF; try an .exe renamed to .pdf (rejected); edit and save extracted text
- [ ] Create a quiz by hand: add all three types, reorder, delete, set timer/points/image/standard code
- [ ] AI generate a quiz and flashcards; review the draft; "Close without saving" discards it
- [ ] Study Ta Bai: generate, differentiate, translate, reading level; edit, save, reopen, delete
- [ ] Host live; join from two other computers (or windows); wrong code and duplicate nickname are refused
- [ ] Start, pause (timer freezes), resume, skip, end; leaderboard and reactions update on all screens
- [ ] Late answers are refused; a student with a 2x time accommodation still can answer
- [ ] Self-paced with a due date: answer, finish, and see answers refused after the due date
- [ ] Flashcard homework: flip with Space, move with arrows
- [ ] Reports: class and student views; export CSV (opens in Excel) and PDF
- [ ] Student: high contrast, larger text (Ctrl +/-), read-aloud, play using only the keyboard

## 9. Known limitations
- Live play is LAN-only (no internet relay, no TLS). Run it on a trusted classroom network.
- `.ser` files are tied to the class definitions; if you change a model class, old data may not load.
- Swing has no live-region announcements for screen readers, so WCAG 2.1 AA is met in intent (contrast, keyboard, scaling, names) but a full screen-reader audit has not been done.
- Question images should be small PNG/JPG/GIF files (up to about 1 MB).
- Scanned PDFs (images only) are not supported.
- Standards shipped are placeholder samples; enter your real framework codes.

## 10. Possible next improvements
- Internet relay or QR-code join link; TLS for sockets
- OCR for scanned PDFs; import/export of quizzes
- Team mode and more game modes through new `ScoringStrategy` classes
- Auto-advance when everyone has answered; per-question result charts
- Screen-reader announcements via custom accessible roles; localisation of the UI
- Versioned export format (JSON) alongside the `.ser` files
