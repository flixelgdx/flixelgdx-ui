# FlixelGDX UI (project instructions for AI assistants)

---

## Project context (FlixelGDX UI)

FlixelGDX UI is the UI widget toolkit extension for **[FlixelGDX](https://github.com/flixelgdx/flixelgdx)**, a
general-purpose Java game framework built on bgfx + SDL3 (with WebGL for HTML5). It provides panels with
nine-slice backgrounds, modals, buttons and floating action buttons, text boxes, checkboxes and radio buttons,
dropdowns, and tooltips.

This repository is a **standalone extension**: it is pure Java and depends only on `flixelgdx-core`. It builds
against the framework through a composite build (see `COMPILING.md`) or a published Maven Central / JitPack
coordinate. Runtime verification happens in a separate game project that consumes this extension.

---

## Collaboration before implementation

Treat the interaction as teamwork, not robotic task execution. Prefer brainstorming when the user's direction
is ambiguous.

- Before implementing anything (planning or coding):

  1. Ask yourself whether the requested design or refactor is actually good for FlixelGDX UI.
  2. If it hurts the extension, breaks invariants (especially the "the UI never reads input" rule below), or
     there is clearly a better path, **stop before editing files or running commands**.
  3. Explain why (pros and cons), suggest better alternatives, and ask whether the user still wants to proceed.
  4. If they confirm after that discussion, proceed as requested.

- If you are unsure about something, whether that would be for a library, a specific aspect of the framework's
  or this extension's codebase, etc., do **not** assume anything. Always verify first before making an
  assumption. If you are still unsure about something, ignore it and bring it up to the user when you're done
  completing a task.

---

## Explaining things for beginners and contributors

The extension welcomes new contributors learning open source.

When explaining code or introducing patterns:

- Explain **why** before **how** (motivation before mechanics).
- Use analogies for complex systems; if the user gave no analogy topic, ask for one they like.
- End **complex** explanations with a short check-in question so you can verify understanding.
- Stay encouraging and professional. Assume intelligence but not deep familiarity with Java or FlixelGDX quirks.

---

## UI design rules

These rules are specific to this extension and take priority whenever a design decision touches them.

- **The UI never reads input.** No widget or display code may call `Flixel.mouse`, `Flixel.keys`,
  `Flixel.touches`, `Flixel.gamepads`, action sets, or register input listeners. Every interaction is a public
  method the game calls, such as `hover()`, `press()`, `click()`, `open()`, `select(i)`, or `scroll(n)`. The only
  exceptions are `FlixelTextBox` calling `Flixel.input` to start or stop text input from `focus()` / `blur()`,
  and `FlixelTextBox` registering itself as a `FlixelKeyboardListener` in its constructor (and removing itself
  in `destroy()`). It only acts on keys while focused, and only the game focuses it.
- Widgets implement the `org.flixelgdx.functional` interfaces (`IFlixelBasic`, `FlixelPositional`,
  `FlixelColorable`, and so on). They never extend `FlixelObject` or `FlixelSprite`.
- Image and font parameters take a `FlixelFile`, never a `String` path.
- Layout is anchors plus stack containers. There is no other layout system.
- Users bring their own assets; the extension ships no built-in skin.

---

## Code quality (non-negotiables)

### Performance, memory, and allocations

- **Do not allocate objects inside loops or in methods invoked every frame.** That rule is strict. Prefer reuse,
  pooling, indexed `for` loops, and performance-oriented helpers such as FlixelGDX's `FlixelString` or
  `FlixelMap`.
- **Always put fields in the correct order for each class**. Follow the order below:

  1. `long`s and `double`s
  2. `int`s, `float`s, and object references
  3. `short`s and `char`s
  4. `boolean`s and `byte`s

- **Standard Java collections are completely banned**. They take up too much memory and allocate too many
  objects when they're used. Prefer FlixelGDX `collections` package instead, which are significantly more lean
  and don't allocate garbage when used. The only exception to this rule is build-time tools like plugins, since
  they do not impact a game's performance at runtime and the framework's code can't be accessed at that phase
  anyway.
- **Reflection is banned**. It breaks many platforms that require ahead-of-time compilation and is unstable for
  situations like version bumps. If reflection must be used, don't touch the main area requiring it, and bring
  it up at the end of your task, explaining why it's needed.
- **Do not use deprecated APIs**. If one *is* used, replace it with the modernized, recommended version instead.

### Coding style

**Always put fields, modifiers, types, and methods in the correct order**. This keeps the code readable and
consistent. Follow the orders below:

#### Modifiers

1. `public`
2. `protected`
3. default
4. `private`
5. `static`
6. `final`

#### Fields, methods/functions and types

1. Fields (following the alignment padding rule!)
2. Constructors, with smallest to largest parameters top to bottom
3. Methods (if there are overloads, order them smallest to largest parameters top to bottom)
4. Simple getter/setter methods (below every other method)
5. Inner classes
6. Inner interfaces
7. Inner enums

### Architecture and scope

- Keep changes minimal: avoid unrelated files unless needed for the stated task.
- This extension depends only on `flixelgdx-core`; do not add a dependency on a platform backend
  (`flixelgdx-desktop`, `flixelgdx-html5`, and so on).
- Keep backend or platform quirks out of the widget code; the extension should behave identically on every
  platform FlixelGDX supports.

### Language and style

- Target **Java 17**. Prefer modern features (records, lambdas, modern `switch`) over legacy patterns.
- **Import** every type you use. Do **not** use star imports (`*`).
- Do **not** use fully qualified class names inline when a normal import would read cleanly.
- Empty or void-returning methods: opening brace on the same line per `.editorconfig` (example:
  `public void hook() {}`).
- Prefer **short** field and method names. If shortening a method name hides its meaning, use a concise name
  plus Javadoc instead of a long identifier.

### Finishing work

Before finishing a coding task, run:

1. Compiling: `./gradlew compileJava`
2. Spotless: `./gradlew spotlessApply`
3. Checkstyle: `./gradlew checkstyleMain`
4. Javadocs: `./gradlew javadocAll`
5. Unit tests: `./gradlew test`

Additionally, if you are currently on a branch for a pull request, always update the description of the PR to
ensure accuracy after completing a task.

---

## Documentation, comments, and Javadoc

Documentation should read like a **beginner-friendly handbook**, not an expert-only manual.

### Mechanics

- Use correct grammar and punctuation everywhere (comments, `@param`, `@return`, `@throws`, and so on).
- Stick to **ASCII** in prose when practical; avoid decorative punctuation like en dash, em dash, fancy arrows,
  or emojis. This applies to both source comments and Javadoc. Use a plain hyphen (-) only for compound
  adjectives; never use it as a sentence separator or stand-in for an em dash. This rule also applies to inline
  comments in build scripts. This allows the docs to be read easily on every device and requires you to use
  clarity over brevity. The Markdown docs are the only exception to this rule.
- Use **consistent capitalization and grammar** in prose and code.
- Every doc comment should always start with a single sentence, with detailed paragraphs following.
- Include the right Javadoc tags (`@param`, `@return`, `@throws`, ...) wherever they apply.
- Use nullability annotations (`@Nullable`, `@NotNull`) where they help tooling.
- Keep `@link` references valid or fix broken links.
- Follow `.editorconfig` for formatting.
- Add comments where either complexity would otherwise be hard to follow, or where code requires import
  context.
- Skip Javadoc on trivial, self-explanatory methods (such as plain getters/setters) unless there is subtle
  behavior.
- All source files should carry the project's standard copyright header (exceptions: `package-info.java`,
  `module-info.java` and build scripts).
- Use **American English** in docs. (e.g., "behavior" instead of "behaviour")
- After code changes that affect public behavior or APIs, **update relevant Markdown docs** in the repo.
- Don't use section comments (like `// ---`). The code should be easily navigable simply by how it's organized;
  section comments are just noise.
- If a class needs to be referenced in a `@link`, don't write out the full package: import it as a qualifier.
  This allows the extension's Javadoc links to be easy to read and not a blue mess. (Example: **not**
  `{@link org.flixelgdx.ui.FlixelUiWidget}`, just `{@link FlixelUiWidget}` + `import` for qualifier if needed.)

### Comments versus Javadocs

- For single-line comments, do **not** use Markdown tricks (bold with `**`, backticks around snippets, etc.).
  Reserve richer formatting for Javadoc.
- When naming methods inside comments, include parentheses (`someMethod()`). If parameters exist, but you are
  not spelling them out, use `anotherMethod(...)`. Example class-qualified form: `SomeClass.someMethod(...)`.

### Heavily used or critical APIs

For widely used classes, fields, methods, or anything central to correctness, include a **small usage example**
with an analogy in Javadoc showing correct typical use.

---

## Working with Git and Pull Requests

- Prefer **small, focused commits** as you finish logical slices of work so history stays readable. For example,
  if the task involves a large refactor, **don't dump everything in one commit**; split each logical change into
  a separate commit.
- Use **one branch and one pull request** unless the user explicitly asks for more (for example, stacked
  features or dependent work).
- If the user renames a pull request, **do not rename it back**; respect their title.
- Your commit titles should be **short and descriptive**, not exceeding **72 characters**, and should not
  contain **keywords in front (e.g. `fix`, `feat`, `refactor`, etc.)**. They should also be **present** tense.
  Examples:
    - "Update README with more descriptive content"
    - "Fix typos in documentation and refactor FlixelUiButton"
    - "Fix layout bug in FlixelUiStack"
    - "Add missing Javadoc to FlixelUiDisplay"
- Always pull the latest changes before changing any code if on a branch outside of the `master` branch.
- If the current branch is set to `master` or something else, **create a new branch off of the latest changes
  from `master`**.
- When you're done with a task (and you haven't yet made one), **create a pull request**. Make sure it follows
  the [PR template](.github/PULL_REQUEST_TEMPLATE.md) exactly with all of your changes.
- Pull request titles should be read as **past tense**, in the format as if it was a new update to a game.
  Examples:
    - "Added a nine-slice panel background with configurable insets"
    - "Enhanced the dropdown widget with keyboard-friendly highlight navigation"
    - "Reworked the text box's caret and selection rendering to use per-character metrics"
- All pull requests should target the **`master`** branch.
- If the user has changes present on the current branch, **do not undo, modify or touch them**. Leave them
  as-is.
