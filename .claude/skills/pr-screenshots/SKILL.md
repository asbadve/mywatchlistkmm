---
name: pr-screenshots
description: How to produce before/after screenshots for a visual PR and host them without adding permanent weight to the repo. Apply whenever a PR changes something visible - colours, layout, theming - or when asked to add screenshots to a PR.
---

# Screenshots on a PR

A visual change needs visual evidence. A colour or layout fix that "passes tests" proves nothing -
Compose UI tests assert node existence, so white-on-white text passes every one of them. The
screenshot is the proof.

Two rules, and the second is the one that gets forgotten:

1. **Before *and* after.** An "after" shot alone shows a screen that looks fine. It does not show
   that anything was broken. The pair is the argument.
2. **The images never enter git.** See *Hosting* - and read it before capturing, because the order
   of operations matters and is easy to get wrong irreversibly.

## Capturing the pair

Follow `.claude/skills/run-app/SKILL.md` for the platform mechanics (Android is the only one that
can be UI-driven; note the `screencap` multi-display trap). What that skill does not cover:

```bash
export ANDROID_SERIAL=<serial>
adb shell cmd uimode night no        # or `yes` - capture the theme the change is about
adb shell settings put global window_animation_scale 0

git checkout master                  # BEFORE
./gradlew :composeApp:installDebug   # …navigate, capture…
git checkout <branch>                # AFTER
./gradlew :composeApp:installDebug   # …same screens, same titles, capture…
```

- **Same content on both sides.** Same film, same person, same device, same theme. If the "before"
  shows *Inception* and the "after" shows *Silo*, the pair proves nothing and a reviewer will say so.
- **Downscale**: `sips -Z 900 shot.png`. ~300 KB instead of 1-3 MB, and still legible in a PR.
- **Check both themes** when the change is theming-related - see the standing rule in
  `.claude/skills/run-app/SKILL.md` and the project memory. Ship the theme the change is *about*;
  mention the other.

### Look at what you captured before you publish it

TMDB is not curated. The Person tab's trending list has surfaced adult performers with explicit
thumbnails, because `include_adult=false` is only sent on `/search/multi` - the trending endpoints
have no such filter. **Read every screenshot before attaching it to a public PR**, and pick
different content if needed. A screenshot is a publication.

## Hosting

**Paste the images into a PR comment.** GitHub uploads them to
`github.com/user-attachments/assets/…` - free, permanent for the life of the repo, served on
GitHub's CDN, and **zero git weight**.

This is browser-only. There is no API and `gh` does not wrap it, so an agent cannot do it. Hand the
files to the user and ask for the generated markdown back. That handoff is cheaper than every
alternative - see `docs/screenshot-hosting-options.md` for the full comparison and costs.

Stage the files somewhere easy to drag from, and say exactly which PR needs which:

```bash
OUT="$HOME/Desktop/pr-screenshots"; mkdir -p "$OUT"
cp screenshots/<files> "$OUT/"
git show <other-branch>:screenshots/<file> > "$OUT/<file>"   # files on another branch
open "$OUT"
```

### Committing them to `screenshots/` is a trap

It works, and it is what a first attempt reaches for. The cost is permanent: git history is
append-only, so **deleting the PNGs later does not shrink any clone**. This repo is already 54 MB of
screenshots inside an 84 MB `.git`.

Two follow-on traps:

- **`git rm` in a later commit achieves nothing.** The blobs stay reachable through the earlier
  commit and still merge into `master`. Only dropping those commits - `git reset --hard <sha>` plus
  a force-push - actually removes them.
- **An orphan branch does not help either.** A default `git clone` fetches all refs and their
  objects, so images on a `screenshots` branch still land in every clone. It tidies `master`'s log,
  nothing more.

If images have already been committed and the PR is not yet merged, they can still be rescued -
follow the recovery order below. **After merge, they are in `master` forever.**

## Order of operations (the part that bites)

If the PR body already points at `raw.githubusercontent.com/<sha>/…`, those URLs die the instant the
commit is dropped. Never rewrite history first.

1. User uploads to a PR comment; collect the `user-attachments` URLs.
2. Rewrite both PR bodies to the new URLs. Match on the **full URL including filename**, and replace
   longest filenames first so `foo_before.png` is not shadowed by `foo.png`.
3. **Verify before destroying anything:**
   ```bash
   curl -s -o /dev/null -w "%{http_code} %{size_download}\n" -L "https://github.com/user-attachments/assets/<uuid>"
   ```
   Expect `200` and a byte count matching the local file. Size-matching is the real check - a 200
   with the wrong size means the wrong image is attached.
4. Only then `git reset --hard <code-commit>` and `git push --force-with-lease`.
5. Confirm with `gh pr diff <n> --name-only` that no PNGs remain in the diff.

Use `--force-with-lease`, never `--force`. Check for existing reviews first: rewriting history
orphans review threads, so ask before force-pushing a PR that has been reviewed.

## Writing the PR body

- Put the pairs in a table, `<img src="…" width="300">` per cell. Native markdown images render
  full-bleed and make a long PR unreadable.
- **Caption what to look at**, naming the elements: "the `G · 2026 · 1h 35m · ★ 6.3` row and the
  Play trailer button are ghosts on the left". A reviewer should not have to hunt for the difference.
- **Say which pair is weak.** If one screen barely changed, write that down. Three side-by-sides
  imply three equal fixes; if one is marginal, claiming otherwise is the kind of thing a reviewer
  notices and stops trusting the rest of the description for.
- **Caption the regressions too.** If the fix flattens the artwork, point at it and name the
  constant that tunes it. The screenshot shows it either way - describing it first is the difference
  between a known trade-off and something that looks hidden.
