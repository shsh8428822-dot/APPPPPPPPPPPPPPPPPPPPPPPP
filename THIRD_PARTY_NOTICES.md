# הודעות רישוי – GAME BOX

## Puzzle Suite (304 games)

The GAME BOX APK embeds a web build of **Puzzle Suite** by Sidhant947:
https://github.com/sidhant947/Puzzle

Pinned upstream commit: 3e70679e51509bd669660b5a4556942fba48321a

License: GNU General Public License version 3.0 (GPL-3.0).
The upstream source is fetched at the pinned commit during the Android build.
The build applies the rebranding, green theme and Hebrew localization changes
defined in scripts/prepare_puzzle_suite.py.

The complete GPL-3.0 text is included as LICENSE. The corresponding modified
source can be reproduced from this repository and the pinned upstream source.

This APK also includes GAME BOX's original Android host and 14 native mini-games.

## Additional HTML5 game collections (190 games)

The APK also includes 142 browser games from sausi-7/games:
https://github.com/sausi-7/games
Pinned commit: c97ef8bec4a4ce3154b4345a79aeda3ea2a6a465
License: MIT. The original MIT notice is bundled in app assets at
assets/mini/licenses/SAUSI-MIT.txt. CDN game-engine scripts used by the games
are fetched at build time and stored locally so gameplay does not need internet.

It also includes 48 compact browser games from mashukui/web-games:
https://github.com/mashukui/web-games
Pinned commit: 2770860c6e2b3ab8144a56462d8178fe7e3445c7
License: MIT. The original MIT notice is bundled at
assets/mini/licenses/MASHUKUI-MIT.txt.

These 190 games are combined in the local Hebrew GAME BOX launcher.
