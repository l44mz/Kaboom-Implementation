# Sprint 5: Store, history, admin, push (Nov 8 to 12)

## Sprint planning

**Goal:** complete US-10 (shop), US-12 (history), US-13 and US-14 (admin), SH-01 (pause and resume), plus the third-party integration (push notifications, Holberton Meeting 5). Users can buy and equip items and see their history, the admin can manage products, and push notifications work.

18 cards, 42 points, all due Nov 12. Main cards: products, purchases with a row lock and equip (Noura, 5), Shop screen (Gheed, 5), history endpoint (Noura, 2) and screen (Shahd, 3), admin guard and stats (Noura, 3), admin product endpoints (Lama, 3), Admin Dashboard (Gheed, 3), Add Product screen (Shahd, 2), pause and resume endpoints (Lama, 2) and buttons (Gheed, 2), push notification backend (Lama, 3) and app (Shahd, 2), equipped item on the robot (Shahd, 2), purchase concurrency test (Lama, 2), plus a UI polish card added during the sprint (Shahd).

## Sprint review

**Planned:** 18 cards, 42 points. **Done:** 18 cards, 42 points (100%). The work was merged into `develop` with PR #19.

| Done | Points |
|---|---|
| Products, purchases with row lock, items and equip (Noura) | 5 |
| Shop screen: filters, buy and equip (Gheed) | 5 |
| History endpoint (Noura) and History screen (built by Gheed) | 2 + 3 |
| Admin Dashboard screen (Gheed) | 3 |
| Add Product screen (built by Gheed) | 2 |
| Admin guard, stats and products endpoints (Noura) | 3 |
| Admin product endpoints (Lama) | 3 |
| Pause and resume endpoints (Lama) and buttons (Gheed) | 2 + 2 |
| Push notification when a session ends, backend (Lama) and app (Shahd) | 3 + 2 |
| Equipped item on the robot display (Shahd) | 2 |
| Purchase concurrency test (Lama) | 2 |
| Sprint 5 QA pass (Lama), code reviews (Noura), planning, review and retro (Shahd) | 1 + 1 + 1 |
| UI polish and animations (Shahd, card added mid-sprint) | 0 |

Delivered:
- Shop with filters, buy confirmation and equip; history screen; My Account; Admin Dashboard and Add Product.
- Backend: products, purchases with a row lock, owned items and equip, product images, pause and resume.
- UI polish to match Figma: smaller text, more padding, lavender main button, labels above fields, and Lottie animations (confetti, coin shine, purchase pop-up, loading dots, pairing pulse).
- Sparky explosion on the backend and robot: the robot "explodes" when the user goes away twice or is distracted four times.
- Session notifications in the app with the timer and when a session ends.

Open issue found in review: only "create product" is limited to admins; the other admin endpoints accept any logged-in user.

**Demo:** buying and equipping an item in the Shop, the History screen, the Admin Dashboard and Add Product, and pause and resume during a session.

## Retrospective

**What went well**
- A lot was built: the shop, history, admin and polish all exist and look like Figma.
- Team members picked up each other's cards when someone was ahead (for example, Gheed built the History and Add Product screens).

**What did not go well**
- 0 pull requests were opened during the sprint, so the work was reviewed and merged later (PR #19), and `develop` did not change that week.
- The board was not updated during the sprint, so it showed very few cards done while most of the work was already built.
- Unplanned work (explosion, camera mode) was not added to the board.

**What we change next sprint**
- Every branch gets a PR into the integration branch before the sprint ends.
- A card moves to Done only after review and merge, and the comment links the PR.
- New ideas become cards before anyone starts them.

