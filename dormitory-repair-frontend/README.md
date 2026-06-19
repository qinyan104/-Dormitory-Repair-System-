# Dormitory Repair Frontend

Student/admin frontend for the dormitory repair system.

## Development

Install dependencies:

```bash
npm install
```

Start dev server:

```bash
npm run dev
```

Build:

```bash
npm run build
```

## Stack

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios

## Shared UI Naming

- shared UI components now live in `src/components/ui/`
- shared component names use the `Ui*` prefix
- Key components:
  - `UiMobileTabBar`: dynamic bottom navigation
  - `UiStarDisplay`: graphical 5-star rating with mood emojis
  - `UiTimeline`: vertical visual process timeline
- older `components/mc` / `Mc*` references in `docs/superpowers/` are historical planning records, not the current implementation

## Routes

- `/login`
- `/register`
- `/student/home`
- `/student/repair/create`
- `/student/repair/list`
- `/student/repair/detail/:id`
- `/student/repair/feedback/:id`
- `/student/profile`
- `/admin/dashboard`
- `/admin/repairs`
- `/admin/notices`
- `/admin/users`
- `/admin/categories`
- `/admin/logs`
- `/admin/profile`
- `/repairer/dashboard`
- `/repairer/orders`
- `/repairer/orders/:id`
- `/repairer/profile`

## Auth Notes

- `/` redirects to `/login`.
- Login state is stored in `sessionStorage`.
- Reopening the browser goes back to the login page.

## Repair Order Integration Notes

The backend repair-order API and the student UI do not use the same field names directly.

Backend raw fields:

- `title`
- `categoryId`
- `content`
- `imageUrl`
- `repairStatus`
- `submitTime`
- `finishTime`
- `remark`

Student UI display fields:

- `description`
- `images`
- `categoryName`
- `processRemark`

Normalization is handled in [`src/utils/repair.ts`](C:/Users/HX/Desktop/JavaWeb/dormitory-repair-frontend/src/utils/repair.ts).

If backend response shapes change later, update that file first.

Admin-side repair pages now receive enriched repair-order responses that include:

- `categoryName`
- `username`
- `realName`
- `studentNo`
- `phone`
- `dormitoryBuilding`
- `roomNo`
- `handlerName`

The backend also mirrors:

- `content -> description`
- `remark -> processRemark`

## Repair APIs Used by Frontend

- Auth: `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/captcha`, `GET /api/auth/me`, `POST /api/auth/logout`
- Create repair: `POST /api/repair-order`
- Student repair list: `GET /api/repair-order/my-page`
- Admin repair list: `GET /api/repair-order/page`
- Repair detail: `GET /api/repair-order/{id}`
- Cancel repair: `PUT /api/repair-order/cancel/{id}`
- Student confirm: `PUT /api/repair-order/student-confirm/{id}`
- Admin accept: `PUT /api/repair-order/accept/{id}`
- Admin assign worker: `PUT /api/repair-order/assign/{id}`
- Admin update status: `PUT /api/repair-order/status/{id}`
- Worker orders: `GET /api/repair-order/worker-page`
- Worker accept: `PUT /api/repair-order/worker-accept/{id}`
- Worker complete: `PUT /api/repair-order/worker-complete/{id}`
- Export orders: `GET /api/repair-order/export`
- Category list (public, enabled only): `GET /api/category/list`
- Category page (admin, all statuses): `GET /api/category/page`
- Create category: `POST /api/category`
- Update category: `PUT /api/category` (id in body)
- Delete category: `DELETE /api/category/{id}`
- Toggle category status: `PUT /api/category/{id}/status`
- User page (admin): `GET /api/user/page`
- Update user status: `PUT /api/user/status/{id}` (body `{status}`)
- Reset user password: `POST /api/user/{id}/reset-password`
- Change password: `POST /api/user/change-password`
- Notice page: `GET /api/notice/page`
- Create notice: `POST /api/notice`
- Update notice: `PUT /api/notice` (id in body)
- Delete notice: `DELETE /api/notice/{id}`
- File upload: `POST /api/file/upload`
- Operation logs: `GET /api/log/page`
- WebSocket: `ws://localhost:8080/ws` (STOMP over SockJS)

## Student Repair Submit Behavior

The student repair form includes `dormitoryBuilding` and `roomNo`, but the backend create endpoint still stores those values on the user profile rather than on `repair_order`.

Before creating an order, the frontend now:

- pre-fills dormitory fields from the logged-in user
- syncs changed dormitory fields back to `PUT /api/user/profile`
- then submits the repair order

Admin repair pages display dormitory data from the current student profile returned by the enriched repair-order API.

## Backend Runtime Note

The Spring Boot backend must run with `JDK 17`.

The current local machine also has `JDK 25`, but Lombok compilation fails there with `TypeTag :: UNKNOWN`.

Recommended local backend runtime:

- `JAVA_HOME=D:\code_tools\JDK17`

## Verification

Frontend production build was verified successfully on `2026-05-01` with:

```bash
npm run build
```

## Backend Test

99 controller tests in `dormitory-repair-backend/src/test/java/.../controller/`:

```bash
cd dormitory-repair-backend
$env:JAVA_HOME="D:\code_tools\JDK17"
mvn test
```
