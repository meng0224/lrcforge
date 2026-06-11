# DESIGN.md

## Reference-specific target for LrcForge home screen

## Reference-specific target for LrcForge home screen

When a reference image is provided for the LrcForge home screen, prefer matching that image’s layout language and color composition over generic Material 3 defaults.

### Overall composition

The target home screen should feel like:
- a tinted full-screen background
- a plain top header with a large title and short subtitle
- three prominent stacked cards
- low visual clutter
- strong spacing rhythm
- minimal nested structure

### Layout rules

The screen should not be structured like a traditional Android settings page.

Preferred structure:
1. plain header
2. first large info card
3. second large settings card
4. third large file/action card

Avoid:
- giant hero cards wrapping the header
- floating bottom action sheets over scrolling content
- many small repeated cards stacked inside larger cards
- settings-list skeletons with only cosmetic Material styling

### Card strategy

Use a small number of large cards.
Each major card should feel like a complete visual block.

Cards should have:
- large rounded corners
- soft tonal fill
- minimal border emphasis
- strong internal padding
- enough vertical spacing between cards

Avoid:
- hard outlines
- dense nested subsections
- cards used only as thin wrappers around other cards

### Color composition

Do not use a generic white/gray Android utility palette.

Prefer:
- one dominant hue family per theme
- tinted background related to the active hue
- clearly differentiated card tones
- controls that inherit the same palette

The result should feel color-composed, not merely “MD3-like”.

### Header

The header should be visually simple:
- large app title
- short subtitle
- generous spacing before the first card

Do not wrap the whole header in a hero card unless explicitly requested.

### File / action area

The file list empty state and the primary CTA should belong to the same major card.

The primary CTA should:
- feel integrated into the card
- be visually dominant
- not float above other content
- not behave like an overlay

### Specific anti-patterns to avoid

- legacy utility-form look
- floating bottom action panel
- card-inside-card-inside-card main structure
- visual clutter added just to look modern
- mixed unrelated colors on the same screen
- long explanatory paragraphs used to fill space

## Purpose

This file defines the visual design system for **LrcForge**.

LrcForge is an Android utility app for batch subtitle conversion to LRC.
This document exists so coding agents can make UI changes that stay visually consistent with the product direction.

This is a **visual design specification**, not an architecture document.

---

## Product Intent

LrcForge should feel:

- clean
- soft
- modern
- utility-focused
- readable
- calm rather than flashy

The target visual language is:

- **Material You / Material Design 3**
- large rounded corners
- tonal container layering
- gentle contrast
- clear typography hierarchy
- generous spacing
- minimal visual noise

The app should **not** look like an old Android tools app with:
- hard gray borders
- flat white blocks everywhere
- cramped spacing
- overly dense settings rows
- inconsistent button/card styles

---

## Core UX Goals

1. Keep the app easy to scan for first-time users.
2. Make primary actions visually obvious.
3. Reduce harsh visual separation.
4. Preserve the current workflow and wording unless explicitly requested.
5. Prefer clarity and calmness over “technical” or “developer tool” aesthetics.

---

## Design Keywords

Use these words as guidance when making design decisions:

- soft
- rounded
- tonal
- layered
- airy
- stable
- readable
- practical
- Android-native
- Material You

Avoid:

- sharp
- boxy
- harsh
- old-fashioned
- over-outlined
- crowded
- desktop-like
- dark-heavy unless dark theme is specifically being implemented

---

## Platform Assumptions

- Android app
- Prefer native Android feel
- Prefer Material 3 visual direction
- Current implementation is likely based on Views/XML and Material Components
- Do not migrate the whole screen to Jetpack Compose unless explicitly requested

---

## Visual Principles

### 1. Use container color hierarchy, not heavy borders

Prefer layered surfaces and tonal containers over strong strokes.

Good:
- card container colors
- subtle elevation
- soft outline only when necessary
- surface / surfaceContainer / secondaryContainer style separation

Bad:
- thick gray rectangular borders
- multiple nested hard-outlined boxes
- every section looking like a form field

### 2. Large corner radius

UI should feel rounded and friendly.

Preferred radius direction:
- cards: large
- buttons: large / pill-like when appropriate
- chips: rounded
- dialogs/sheets: rounded
- small utility elements can be slightly smaller, but still soft

### 3. Strong typography hierarchy

Text must clearly communicate structure.

Each screen should clearly distinguish:
- screen title
- section title
- primary body text
- supporting/help text
- button text
- status/metadata text

Do not make everything look like the same text size and weight.

### 4. Spacious layout rhythm

Use generous vertical spacing between major sections.
Avoid cramped rows and overpacked cards.

### 5. One obvious primary action per area

Primary buttons should be visually dominant.
Secondary actions should not compete with the main action.

---

## Color System

### Color Strategy

Prefer a **Material You style palette**:
- neutral background
- soft tinted containers
- one clear primary accent
- optional secondary tint for supporting panels
- avoid saturated rainbow UI

### Default Visual Direction

The baseline theme should feel like:
- cool neutral background
- soft blue primary
- pale blue secondary containers
- subtle tonal separation between sections

### Semantic Roles

Use semantic roles rather than raw colors whenever possible:

- `primary`: main call-to-action, active toggles, key accents
- `onPrimary`: text/icons on primary backgrounds
- `secondary`: supportive accents, less dominant emphasis
- `background`: app background
- `surface`: main panels and cards
- `surfaceVariant` / `surfaceContainer*`: layered containers
- `outline`: subtle separators only
- `error`: validation and failure states

### Color Behavior Rules

- Background should not be pure stark white if a softer neutral is available.
- Cards should usually use a slightly tinted container instead of pure white.
- Primary buttons should use filled or tonal-filled Material styles.
- Outline color should be subtle and never dominate the composition.
- Do not use strong black borders around large containers.

---

## Typography

### Tone

Typography should feel:
- modern
- highly readable
- Android-native
- not overly playful
- not ultra-condensed

### Hierarchy

#### Display / Screen Title
Used for:
- page titles like “LrcForge”
- major top-level headings

Should be:
- visually prominent
- bold or semibold
- clearly separated from body content

#### Section Title
Used for:
- groups like “儲存位置”, “文件列表”

Should be:
- strong
- easy to scan
- noticeably smaller than screen title

#### Body
Used for:
- normal setting descriptions
- explanatory copy
- file state text

Should be:
- readable at a glance
- never too thin
- not oversized

#### Supporting Text
Used for:
- helper descriptions
- state hints
- format support labels

Should be:
- lower emphasis than body
- still readable
- never too faint

#### Button Label
Should be:
- concise
- centered
- medium or semibold feeling
- highly legible on filled buttons

---

## Spacing

Use a clean spacing rhythm.

### General Guidance

- Large spacing between major sections
- Moderate spacing inside cards
- Small spacing only for tightly related text lines

### Layout Rules

- The screen should breathe vertically
- Cards should have comfortable internal padding
- Text should not sit too close to card edges
- Primary buttons should have enough height and horizontal padding to feel touch-friendly and important

---

## Shapes

### Cards
- large rounded corners
- soft container background
- minimal border usage
- use elevation or tonal contrast instead of harsh outlines

### Buttons
- primary action buttons should be filled and rounded
- large-width CTA buttons are preferred for main actions
- secondary buttons may be tonal or outlined, but should still match the soft MD3 language

### Switches
- use Material switch styling
- active state should clearly match the primary palette
- switches should not feel detached from surrounding typography and spacing

### Chips / Badges
- rounded
- simple
- low visual complexity
- use for formats or small metadata only

---

## Component Guidance

## Screen Header

The top of the screen should establish:
- app identity
- purpose
- calm hierarchy

Recommended structure:
- app title
- short subtitle / purpose
- then content cards below

Avoid:
- making the header visually too tiny
- making the first card start too close to the title

---

## Section Cards

Settings and functional groups should be presented as **cards**, not generic bordered blocks.

Each section card should:
- group related settings clearly
- have soft background contrast
- use clean internal dividers only when necessary
- feel like a single intentional module

Avoid:
- box-inside-box-inside-box
- strong outline on every child item
- overly technical table-like styling

---

## Settings Rows

A settings row should contain:
- label
- optional description
- trailing control (switch / value / chip)

Rules:
- label must be immediately scannable
- description must be readable but secondary
- trailing control must align cleanly
- vertical spacing should prevent crowding

Use separators sparingly.
Prefer spacing before using divider lines.

---

## File List Empty State

The empty state is important and should feel designed, not placeholder-like.

Expected style:
- card-based panel
- centered emphasis
- one clear file-type badge or visual indicator
- strong empty-state title
- supporting text below
- enough breathing room

The empty state should feel intentional and polished.

Avoid:
- plain blank rectangles
- overly technical error-like boxes
- text jammed into the center without hierarchy

---

## Primary Action Area

The main action button (for example, selecting a folder or choosing files) should feel unmistakably primary.

Expected style:
- large filled button
- full width or nearly full width
- generous height
- rounded corners
- clear separation from surrounding containers

Do not style the main CTA like a tiny utility button.

---

## Icons

Icons should be:
- simple
- Material-aligned
- functional
- secondary to content

Avoid decorative overload.
Use icons to support scanning, not to decorate every row.

---

## Motion

Keep motion subtle and Android-native.

Prefer:
- standard Material motion
- short, soft transitions
- simple state changes

Avoid:
- flashy animation
- dramatic spring effects
- decorative motion unrelated to user intent

---

## Tone by Screen Type

### Utility / Settings Screens
- calm
- structured
- softly layered
- readable

### Selection / Action Screens
- primary action should stand out
- instructions should stay compact
- empty states should feel helpful, not dead

### Progress / Result States
- use clear state hierarchy
- success should feel calm and confident
- errors should be visible but not visually chaotic

---

## Do / Don’t

### Do

- use Material You / MD3 design language
- use large rounded cards
- use tonal containers
- create strong text hierarchy
- give major actions strong visual priority
- preserve the app’s utility-first clarity
- keep the UI native to Android expectations

### Don’t

- do not make the UI look like a legacy Android settings screen
- do not use thick gray outlines as the main separation method
- do not compress spacing to fit more on screen
- do not add visual clutter
- do not redesign flows unless explicitly requested
- do not migrate everything to Compose unless explicitly requested
- do not introduce web-style aesthetics that feel foreign on Android

---

## Implementation Guidance for Coding Agents

When editing UI:

1. Read this file before changing layout or theme.
2. Preserve behavior, logic, strings, and workflow unless the task explicitly says otherwise.
3. Prefer incremental visual refactors over large rewrites.
4. Reuse theme/style resources where possible.
5. Centralize colors, shapes, and text appearances instead of hardcoding values repeatedly.
6. Keep the result close to Material 3 visual language even if the project is still on Views/XML.
7. For Android Views, prefer improving:
   - theme tokens
   - shape drawables
   - text appearances
   - spacing in layout XML
   - Material button/card/switch styles
8. When unsure, choose the option that is:
   - softer
   - simpler
   - clearer
   - more native
   - less noisy

---

## Preferred Visual Reference

Use the following reference style direction:

- similar to modern Material You gallery screenshots
- soft cards with tonal backgrounds
- rounded switches and controls
- clear, lightweight hierarchy
- minimal borders
- more “designed panel” than “utility form”

For LrcForge specifically:
- the current functionality stays the same
- the visual target is closer to a polished Material You tool app
- the design should feel intentional enough that screenshots look cohesive without additional explanation

---

## Scope Boundaries

This file controls:
- colors
- shapes
- typography
- spacing
- component presentation
- visual hierarchy
- tone of interface

This file does **not** automatically authorize:
- feature changes
- workflow rewrites
- architectural rewrites
- migration to another UI framework
- changing business logic
- removing existing functionality

---

## Definition of Done for UI Tasks

A UI task is only complete when:

- the screen clearly reflects Material You / MD3 direction
- major sections are visually grouped through tonal containers
- borders are no longer the dominant structure
- primary actions are obvious
- text hierarchy is clear
- spacing feels intentional
- the screen looks cohesive in screenshots
- functionality remains unchanged unless explicitly requested