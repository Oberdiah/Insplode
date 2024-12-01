# Insplode

Repository for the Insplode codebase.

**This codebase is a mess.**

It was written over the course of a couple months, and during the end I was aiming for
feature-completion, rather than tidiness. I may go back and fix it up at some point, but I probably
won't.

- There are globals everywhere.
- There is no style guide.
- The folder structure is loose-to-non-existent, what goes where is best-effort.
- Global constants can be in camelCase or SCREAMING_SNAKE_CASE depending on the mood or phase of the
  moon.
- Sometimes assigning values to globals has consequences, sometimes it doesn't.
- Sometimes initializing objects requires an extra init() call, sometimes it doesn't.
- Comments are a luxury, and when they do exist they can't decide on their style - `//` or `/**`
- UI animation is done in about three different ways
- Rendering phases are ad-hoc and nonsensical.
- Sometimes animation is framerate-dependant, it's a bit of a pot-luck.
- Render thread? Tick thread? Here we only do the main thread.
- Sometimes very tick-looking stuff gets into the render functions.
- All buttons and touch interactions are written from scratch in each instance, hoping they're not
  stepping on each other. UI Libraries? What're those?

All that said, it's still a functional game and I'm pleased it's finished.

\- Oberdiah

P.S. If anyone ever actually wants to add to this mess, pull requests are open.

### Notes

If building on iOS, XCode has changed where provision profiles are stored and RoboVM hasn't caught
up,
you'll need to symlink the folder for it to work:
`ln -s ~/Library/Developer/Xcode/UserData/Provisioning\ Profiles ~/Library/MobileDevice/`
To install onto a device running IOS 17+:
`https://github.com/MobiVM/robovm/issues/755#issuecomment-2108521622`