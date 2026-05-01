# ─────────────────────────────────────────────────────────────────────────────
# Angular 21 — SSR (Server-Side Rendering) Dockerfile
#
# angular.json analysis:
#   project name  : "angular"          → dist/angular/
#   outputMode    : "server"           → SSR enabled, NOT a static site
#   ssr.entry     : "src/server.ts"    → Express server bundle
#   builder       : "@angular/build:application"
#
# Build output layout (dist/angular/):
#   browser/   — static assets (JS, CSS, fonts, images) served directly
#   server/    — Node.js SSR bundle (server.mjs) — must run in Node, not nginx
#
# 3-stage build:
#   deps    → npm ci (cached; re-runs only when package-lock.json changes)
#   builder → ng build --configuration production
#   runtime → Node 22 slim + dist/ only (no source code, no dev dependencies)
# ─────────────────────────────────────────────────────────────────────────────


# ── Stage 1: DEPS ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./

# --ci             : reproduces exact versions from package-lock.json (no drift)
# --ignore-scripts : skips postinstall hooks (security hardening + speed)
RUN npm ci --ignore-scripts


# ── Stage 2: BUILD ────────────────────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

# Reuse the node_modules layer from deps — avoids reinstalling on source changes
COPY --from=deps /app/node_modules ./node_modules

# Copy source after node_modules so a source-only change hits cache at this layer
COPY . .

# Production SSR build.
# angular.json → defaultConfiguration is already "production" but we're explicit.
# Output structure:
#   dist/angular/browser/          ← hashed static assets (served by Express)
#   dist/angular/server/server.mjs ← bundled Express SSR entry point
RUN npx ng build --configuration production


# ── Stage 3: RUNTIME ──────────────────────────────────────────────────────────
# node:22-slim (not alpine) — slim is ~80 MB and avoids musl/glibc edge cases
# that sometimes affect native Node addons on alpine.
FROM node:22-slim AS runtime

# Create app directory and assign to the built-in non-root 'node' user (uid 1000)
RUN mkdir -p /app && chown node:node /app

WORKDIR /app

# Switch to non-root before any COPY so files land with correct ownership
USER node

# ── Copy build output ─────────────────────────────────────────────────────────
# Only the compiled dist/ lands in the runtime image.
# Source code, node_modules devDeps, and Angular CLI are NOT copied.
COPY --from=builder --chown=node:node /app/dist/angular ./dist/angular

# ── Production dependencies ───────────────────────────────────────────────────
# Angular's build tool bundles server.mjs with all its dependencies, so this
# step is usually a no-op. It is kept here in case your src/server.ts imports
# any external runtime packages (e.g. a custom middleware or DB client).
COPY --from=builder --chown=node:node /app/package.json /app/package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts

# ── Environment ───────────────────────────────────────────────────────────────
# PORT is read by the generated Express server in server.mjs.
# Override at container start: docker run -e PORT=4200
ENV PORT=4200
ENV NODE_ENV=production

# ── Healthcheck ───────────────────────────────────────────────────────────────
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:4200/ > /dev/null || exit 1

EXPOSE 4200

# Angular CLI outputs the SSR server as dist/angular/server/server.mjs —
# a self-contained Express app. Running it directly is all that's needed.
CMD ["node", "dist/angular/server/server.mjs"]