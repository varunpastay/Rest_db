# Project Overview - Restaurant QR Ordering & Management System

A single-restaurant, single-owner dine-in ordering system: customers scan a
QR code at their table to browse the menu and order with no login; the
owner logs in to one dashboard that handles everything - accepting and
serving orders, billing, menu management, tables/QR codes, settings, taxes,
discounts, sales reports, and backups.

## Roles

| Role | Login? | What they do |
|---|---|---|
| **Customer** | No | Scan table QR -> browse menu -> order -> track status -> raise help requests |
| **Owner** | Yes (single account) | Everything else - live order board, billing, menu, tables, settings, reports |

There is intentionally no separate kitchen/counter/admin/staff role - one
person runs the whole operation from one dashboard.

## Technology stack

### Backend
| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Web layer | Spring MVC (`@Controller` / `@RestController`) |
| Persistence | Spring Data JPA + Hibernate 6.5 (ORM) |
| Database | MySQL 8 (via `mysql-connector-j`) |
| Authentication | Spring Security 6 - form login, session-based, BCrypt password hashing, CSRF protection |
| Real-time transport | Spring WebSocket + STOMP/SockJS (`/ws` endpoint) - server side is wired up; the dashboard currently uses polling by default (see "Known gaps" below) |
| PDF generation | OpenPDF (invoices, sales-report exports) |
| Spreadsheet generation | Apache POI (sales-report Excel exports) |
| QR code generation | ZXing (`core` + `javase`) |
| Boilerplate reduction | Lombok (`@Getter`/`@Setter`/`@Builder`, etc.) |
| Build tool | Maven (with Maven Wrapper - `./mvnw`, no local Maven install needed) |

### Frontend
| Layer | Technology |
|---|---|
| Templating | Thymeleaf (server-rendered HTML, no separate frontend build) |
| CSS framework | Bootstrap 5 + Bootstrap Icons |
| JavaScript | Plain vanilla JS (`fetch()` for AJAX) - no npm, no bundler, no framework (React/Vue/etc not used) |
| Real-time UI updates | Polling (`setInterval` + `fetch`) every 5 seconds on the owner dashboard |
| Custom styling | `theme.css` (CSS variables for the brand color, light/dark mode via `data-bs-theme`) |

### Data & file storage
- All images (food photos, restaurant logo/banner, generated QR codes) are
  stored as **BLOB rows in MySQL** (`uploaded_file` table), not on disk -
  the app is stateless and portable, no persistent volume/filesystem needed
  on the deployment host.
- Session data (the customer's cart, which table they're bound to) lives in
  the **HTTP session** - in memory by default, so a server restart clears
  active carts (acceptable for a dine-in QR-ordering flow).

### Architecture / package layout
```
com.restro
├── entity/       JPA entities (16 tables - see sql/schema.sql)
├── Repo/         Spring Data JPA repository interfaces
├── Service/      Business-logic interfaces
├── Impl/         @Service implementations of those interfaces
├── controller/   @Controller / @RestController classes (customer-facing + owner-facing)
├── security/     Spring Security config, Owner UserDetails
├── dto/          Cart/CartItem session objects
├── util/         QR generation, invoice/report numbering, Excel/PDF export helpers
└── config/       WebSocket config, GlobalControllerAdvice, first-run DataSeeder
```

Field-level `@Autowired` dependency injection is used throughout (matching
the reference project format this was built to), with a clean
interface/implementation split for every service.

## Database

16 tables, all documented with exact column definitions in
**`sql/schema.sql`** - a hand-written, reviewable migration that matches
the JPA entities column-for-column (useful for production deployments where
you don't want Hibernate auto-altering a live schema). `sql/seed-demo-menu.sql`
loads the demo menu (6 categories, 17 dishes) ported from the original
project, plus standard CGST/SGST tax and a sample discount code.

By default (`spring.jpa.hibernate.ddl-auto=update`), Hibernate creates and
evolves the schema automatically on startup - convenient for development.
**For production, switch this to `validate`** after applying `schema.sql`
manually, so Hibernate checks against a known-good schema instead of
silently altering a live table.

## URLs

- **Customer**: no fixed URL - each table gets a unique QR link
  (`/t/{tableId}/{qrToken}`) generated from **Owner -> Tables & QR Codes**.
- **Owner login**: `/owner/login`
- **Owner dashboard**: `/owner/dashboard` (redirected here after login)

## Key design decisions worth knowing before you deploy

1. **Single restaurant, single owner, single MySQL database per
   deployment.** There's no multi-tenancy - if you need to run this for
   multiple restaurants, you'd deploy one instance (with its own database)
   per restaurant.
2. **Order/invoice numbering uses an in-memory counter** (`OrderNumberUtil`,
   `InvoiceNumberUtil`), not a DB sequence. Fine for a single app instance;
   do not run multiple instances of this app behind a load balancer without
   changing this, or two instances could hand out the same number.
3. **Delete operations are deliberately "safe by default."** Anything with
   real order history (tables, menu items, discounts) refuses to be
   deleted with a clear on-screen message rather than a raw database error
   - because those rows are still referenced by historical orders via
   non-null foreign keys. Use the Active/Inactive or Available/Unavailable
   toggle instead for anything that's been used. Categories can't be
   deleted while they still contain menu items (move or delete the items
   first).
4. **CSRF protection is on everywhere** it matters. Owner-dashboard
   `fetch()` calls (Accept/Serve/Pay/Cancel/Resolve) attach the CSRF token
   via `<meta>` tags read by `/assets/js/csrf.js`; customer-facing
   endpoints (cart, order placement, assistance requests) are explicitly
   exempt since there's no authenticated session to protect there.
5. **`spring.jpa.open-in-view=true`** is intentionally kept on (Spring
   Boot's own default) - controllers and Thymeleaf templates in this app
   access lazy-loaded JPA relationships directly, which requires the
   Hibernate session to stay open through view rendering.
6. **Backup/restore (Settings > Backup) covers configuration only** -
   restaurant profile, branding, menu, tables, taxes, discounts - not order
   history or the owner login. It's a safe merge/upsert (matches by
   name/code, never deletes). For full data protection including order
   history, rely on your hosting provider's regular database backups.

## Known gaps / intentional follow-up work

- The WebSocket/STOMP broker is configured and running (`/ws`), and the
  server pushes order/assistance-request change events to it, but the
  owner dashboard's JavaScript doesn't have a STOMP client wired in yet -
  it still uses 5-second polling, which works fine but isn't instant. Wire
  in `@stomp/stompjs` + SockJS client-side if you want push updates
  instead.
- `MenuService.removeImage()` (delete one specific photo from a food item
  that has several) exists and works, but has no controller endpoint or UI
  wired to it yet - a food item's photo is currently managed as a single
  upload-to-replace field on the item form, not a per-photo gallery. Safe
  to call once wired up (no foreign key blocks it).
- No automated tests.
- Single-instance deployment assumed (see numbering caveat above).

## Deployment checklist

1. Provision a MySQL 8 database.
2. Apply `sql/schema.sql` (recommended for production) or let
   `ddl-auto=update` create it on first boot (fine for a quick deploy/demo).
3. Set real values for `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `APP_BASE_URL` (must be the actual public URL customers' phones can
   reach, since it's baked into every generated QR code),
   `OWNER_DEFAULT_EMAIL`, `OWNER_DEFAULT_PASSWORD`.
4. Put the app behind HTTPS (a reverse proxy like Nginx/Caddy, or your
   host's load balancer) - it doesn't terminate TLS itself.
5. Log in as owner and **change the default password immediately** via
   "My Account".
6. `./mvnw clean package` and run the resulting jar, or `./mvnw spring-boot:run`
   for a quick start.
