# Plagx Writing Companion for Eclipse

**Free academic writing checks in the Eclipse IDE.** Plagx Writing Companion checks essays, theses and papers written in Markdown, plain text or LaTeX: citations and reference lists, BibTeX files, word limits, basic grammar and punctuation, and overall writing readiness.

By Plagiarism Checker X. For full plagiarism checks and AI-generated text detection, visit [Text Similarity Detector](https://plagiarismcheckerx.com/?utm_source=plagx-eclipse-github). This free plugin does not detect plagiarism or AI-generated text.

---

## Installation

**Eclipse Marketplace:** **Help → Eclipse Marketplace…**, search for **Plagx Writing Companion** and click **Install**.

**Update site:** **Help → Install New Software… → Add…** and enter:

```
https://plagiarismcheckerx.github.io/plagx-writing-companion-eclipse/
```

Requires Eclipse 2024-03 (4.31) or newer and Java 17.

## Features

### Draft checks on save
Save any `.md`, `.txt` or `.tex` file and suggestions appear as problem markers in the editor and the **Problems** view:

- **Citations and references:** in-text citations with no matching reference entry, references that are never cited, a missing reference section, and *et al.* formatting (APA or MLA)
- **Word limits:** warnings when the draft goes over or under your minimum and maximum word counts (the reference list isn't counted)
- **Punctuation:** double spaces, spaces before punctuation, missing spaces after punctuation, repeated punctuation, unmatched brackets and quotes, mixed quote styles
- **Basic grammar:** repeated words, *a/an* mistakes, lowercase *i*, sentence capitals, commonly confused words
- **Structure:** long sentences, long or single-sentence paragraphs, and required headings
- **Passive voice** (optional)

Many suggestions have a **Quick Fix** (`Ctrl+1`, or right-click the marker in the Problems view).

### Citations and bibliography
- **BibTeX checks:** duplicate entries, missing authors, titles and years, and invalid DOIs are marked in `.bib` files
- **Format BibTeX as Reference List:** an APA reference list or MLA Works Cited page, ready to copy
- **Check BibTeX References Online:** also verifies every DOI on Crossref
- **Insert Reference from DOI:** insert a formatted reference, an in-text citation or a narrative citation

## Commands

All commands are in the **Plagx** menu:

| Command | What it does |
|---|---|
| Check Draft | Checks the current editor, including unsaved changes |
| Show Writing Readiness | Ready / Not ready, word count, reading time, readability, errors and warnings |
| Format BibTeX as Reference List | Formatted APA or MLA list for the current `.bib` file |
| Check BibTeX References Online | Checks the `.bib` file and verifies DOIs on Crossref |
| Insert Reference from DOI | Looks up a DOI and inserts a reference or citation |

## Preferences

**Window → Preferences → Plagx Writing Companion**: citation style (APA or MLA), minimum and maximum words, required headings, file extensions, check on save, and which check groups run.

## Privacy

All draft and BibTeX checks run locally inside Eclipse. Your text is never uploaded. The plugin connects to the internet only when you ask it to: **Insert Reference from DOI** and **Check BibTeX References Online** send DOIs to the public [Crossref API](https://www.crossref.org/). No accounts, API keys or telemetry are used.

## How it works

The plugin is a thin Eclipse layer over two open-source Plagx libraries from Maven Central, bundled in the plugin:

- [plagx-preflight](https://central.sonatype.com/artifact/io.github.plagiarismcheckerx/plagx-preflight): draft checks, word counts, citation matching, grammar and punctuation
- [plagx-cite](https://central.sonatype.com/artifact/io.github.plagiarismcheckerx/plagx-cite): APA and MLA formatting, BibTeX, DOI lookup and duplicate detection

The checks match the other Plagx tools exactly. Suggestions are rule-based, so always check them against your institution's style guide.

## What this plugin does not do

Plagx Writing Companion gives writing suggestions only. It does **not** check for plagiarism or detect AI-generated text. For full document checks, use [Text Similarity Detector](https://plagiarismcheckerx.com/?utm_source=plagx-eclipse-github) or [download Plagiarism Checker X](https://plagiarismcheckerx.com/download?utm_source=plagx-eclipse-github) for desktop.

## Development

```bash
./mvnw clean verify
```

builds the plugin, runs the headless Eclipse tests and writes the p2 update site to `com.plagiarismcheckerx.plagx.eclipse.site/target/repository`. Pushing to `main` publishes the update site to GitHub Pages.

## License

[MIT](LICENSE) © 2026 Plagiarism Checker X
