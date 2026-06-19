<script setup lang="ts">
import { RouterLink } from 'vue-router'

interface AuthStep {
  index: string
  title: string
  description?: string
}

interface Props {
  mode?: 'login' | 'register'
  eyebrow: string
  title: string
  accent?: string
  subtitle: string
  steps: ReadonlyArray<AuthStep>
  panelTag: string
  panelTitle: string
  panelDescription: string
  footerText: string
  footerLinkText: string
  footerTo: string
}

withDefaults(defineProps<Props>(), {
  mode: 'login',
  accent: ''
})
</script>

<template>
  <div class="auth-shell" :class="`is-${mode}`">
    <aside class="auth-shell__story">
      <div class="auth-shell__brand">
        <span class="auth-shell__brand-mark" aria-hidden="true">
          <i></i>
          <i></i>
        </span>
        <span class="auth-shell__brand-name">宿舍服务中心</span>
        <span class="auth-shell__brand-code">CAMPUS / 07</span>
      </div>

      <div class="auth-shell__story-copy">
        <span class="auth-shell__eyebrow">{{ eyebrow }}</span>
        <h1 class="auth-shell__title">
          {{ title }}
          <em v-if="accent">{{ accent }}</em>
        </h1>
        <p class="auth-shell__subtitle">{{ subtitle }}</p>
      </div>

      <div class="auth-shell__dispatch" aria-label="宿舍报修处理流程">
        <div class="auth-shell__route" aria-hidden="true">
          <span class="auth-shell__route-line"></span>
          <span class="auth-shell__route-signal"></span>
        </div>

        <ol class="auth-shell__steps">
          <li
            v-for="(step, index) in steps"
            :key="step.index"
            class="auth-shell__step"
            :style="{ '--step-delay': `${160 + index * 90}ms` }"
          >
            <span class="auth-shell__step-dot" aria-hidden="true"></span>
            <div class="auth-shell__step-copy">
              <span class="auth-shell__step-index">{{ step.index }}</span>
              <strong>{{ step.title }}</strong>
              <p v-if="step.description">{{ step.description }}</p>
            </div>
          </li>
        </ol>
      </div>

      <div class="auth-shell__story-footer">
        <span>统一身份入口</span>
        <span>学生 · 管理员 · 维修人员</span>
      </div>
    </aside>

    <main class="auth-shell__workspace">
      <section class="auth-shell__form-stage">
        <div class="auth-shell__panel-meta">
          <span>{{ panelTag }}</span>
          <span>{{ mode === 'login' ? 'SECURE ACCESS' : 'ACCOUNT SETUP' }}</span>
        </div>

        <header class="auth-shell__panel-header">
          <h2>{{ panelTitle }}</h2>
          <p>{{ panelDescription }}</p>
        </header>

        <div class="auth-shell__panel-body">
          <slot />
        </div>

        <p class="auth-shell__footer">
          <span>{{ footerText }}</span>
          <RouterLink :to="footerTo">{{ footerLinkText }}</RouterLink>
        </p>
      </section>

      <p class="auth-shell__workspace-note">Dormitory Repair Management System</p>
    </main>
  </div>
</template>

<style scoped>
.auth-shell {
  --story-width: minmax(430px, 44%);
  display: grid;
  grid-template-columns: var(--story-width) minmax(0, 1fr);
  min-height: 100dvh;
  color: var(--mc-ink);
  background: var(--mc-canvas);
}

.auth-shell.is-register {
  --story-width: minmax(350px, 32%);
}

.auth-shell__story {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: clamp(34px, 5vh, 68px);
  min-height: 100dvh;
  padding: clamp(30px, 4vw, 58px);
  overflow: hidden;
  color: var(--mc-white);
  background: var(--mc-ink);
}

.auth-shell__story::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background:
    linear-gradient(rgba(255, 255, 255, 0.042) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.042) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(to bottom, black 20%, transparent 92%);
}

.auth-shell__story::after {
  content: '07';
  position: absolute;
  right: -0.04em;
  bottom: -0.24em;
  z-index: -1;
  color: rgba(255, 255, 255, 0.035);
  font-size: clamp(15rem, 32vw, 34rem);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.1em;
  pointer-events: none;
}

.auth-shell.is-register .auth-shell__story::after {
  content: 'ID';
  font-size: clamp(12rem, 24vw, 25rem);
}

.auth-shell__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.auth-shell__brand-mark {
  position: relative;
  width: 44px;
  height: 26px;
  flex: 0 0 auto;
}

.auth-shell__brand-mark i {
  position: absolute;
  top: 1px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.auth-shell__brand-mark i:first-child {
  left: 0;
  z-index: 1;
  background: #eb001b;
}

.auth-shell__brand-mark i:last-child {
  right: 0;
  z-index: 2;
  background: #f79e1b;
}

.auth-shell__brand-name {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.auth-shell__brand-code {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.42);
  font-family: "JetBrains Mono", "Cascadia Mono", monospace;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.auth-shell__story-copy {
  max-width: 42rem;
}

.auth-shell__eyebrow {
  display: block;
  margin-bottom: 18px;
  color: var(--mc-signal-light);
  font-family: "JetBrains Mono", "Cascadia Mono", monospace;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.auth-shell__title {
  max-width: 10ch;
  margin: 0;
  font-size: clamp(3.4rem, 6vw, 6.6rem);
  font-weight: 650;
  line-height: 0.94;
  letter-spacing: -0.07em;
  text-wrap: balance;
}

.auth-shell.is-register .auth-shell__title {
  font-size: clamp(3rem, 4.7vw, 5.4rem);
}

.auth-shell__title em {
  display: block;
  margin-top: 0.12em;
  color: #f6b797;
  font-style: normal;
  font-weight: 450;
}

.auth-shell__subtitle {
  max-width: 31rem;
  margin: 24px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 15px;
  line-height: 1.8;
  text-wrap: pretty;
}

.auth-shell__dispatch {
  position: relative;
  align-self: end;
  max-width: 680px;
}

.auth-shell__route {
  position: absolute;
  top: 11px;
  right: 9%;
  left: 4%;
  height: 2px;
}

.auth-shell__route-line,
.auth-shell__route-signal {
  position: absolute;
  inset: 0;
}

.auth-shell__route-line {
  background: rgba(255, 255, 255, 0.15);
}

.auth-shell__route-signal {
  width: 58%;
  background: var(--mc-signal-light);
  transform-origin: left;
  animation: routeIn 1s 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.auth-shell__steps {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.auth-shell__step {
  min-width: 0;
  opacity: 0;
  animation: stepIn 0.55s var(--step-delay) cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.auth-shell__step-dot {
  display: block;
  width: 22px;
  height: 22px;
  margin-bottom: 18px;
  border: 6px solid var(--mc-ink);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.38);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.25);
}

.auth-shell__step:first-child .auth-shell__step-dot,
.auth-shell__step:nth-child(2) .auth-shell__step-dot {
  background: var(--mc-signal-light);
  box-shadow: 0 0 0 1px var(--mc-signal-light);
}

.auth-shell__step-copy {
  display: grid;
  gap: 6px;
}

.auth-shell__step-index {
  color: rgba(255, 255, 255, 0.38);
  font-family: "JetBrains Mono", "Cascadia Mono", monospace;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.auth-shell__step strong {
  font-size: 14px;
  font-weight: 650;
}

.auth-shell__step p {
  max-width: 16rem;
  margin: 0;
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
  line-height: 1.6;
}

.auth-shell.is-register .auth-shell__route {
  top: 0;
  bottom: 0;
  left: 10px;
  width: 2px;
  height: auto;
}

.auth-shell.is-register .auth-shell__route-signal {
  width: auto;
  height: 62%;
  transform-origin: top;
  animation-name: routeInVertical;
}

.auth-shell.is-register .auth-shell__steps {
  grid-template-columns: 1fr;
  gap: 22px;
}

.auth-shell.is-register .auth-shell__step {
  display: grid;
  grid-template-columns: 22px 1fr;
  gap: 18px;
}

.auth-shell.is-register .auth-shell__step-dot {
  margin: 0;
}

.auth-shell.is-register .auth-shell__step-copy {
  margin-top: -2px;
}

.auth-shell__story-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.42);
  font-size: 12px;
}

.auth-shell__workspace {
  position: relative;
  display: grid;
  place-items: center;
  min-width: 0;
  padding: clamp(56px, 7vw, 108px);
  background:
    linear-gradient(90deg, rgba(20, 20, 19, 0.035) 1px, transparent 1px),
    var(--mc-canvas);
  background-size: 96px 100%;
}

.auth-shell__form-stage {
  width: min(100%, 430px);
  animation: formIn 0.7s 0.1s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.auth-shell.is-register .auth-shell__form-stage {
  width: min(100%, 720px);
}

@media (min-width: 861px) {
  .auth-shell.is-register .auth-shell__story {
    gap: clamp(20px, 3vh, 38px);
    padding: clamp(26px, 3vw, 42px);
  }

  .auth-shell.is-register .auth-shell__title {
    font-size: clamp(2.7rem, 4vw, 4.7rem);
  }

  .auth-shell.is-register .auth-shell__subtitle {
    margin-top: 16px;
    font-size: 14px;
    line-height: 1.65;
  }

  .auth-shell.is-register .auth-shell__steps {
    gap: 16px;
  }

  .auth-shell.is-register .auth-shell__workspace {
    align-items: center;
    padding: 24px clamp(38px, 5vw, 72px);
  }

  .auth-shell.is-register .auth-shell__panel-header {
    margin-top: 16px;
  }

  .auth-shell.is-register .auth-shell__panel-header h2 {
    font-size: clamp(2.15rem, 3vw, 3.15rem);
  }

  .auth-shell.is-register .auth-shell__panel-header p {
    margin-top: 8px;
    line-height: 1.55;
  }

  .auth-shell.is-register .auth-shell__panel-body {
    margin-top: 18px;
  }

  .auth-shell.is-register .auth-shell__footer {
    margin-top: 14px;
  }

  .auth-shell.is-register :deep(.auth-form) {
    gap: 12px;
  }

  .auth-shell.is-register :deep(.auth-fieldset) {
    gap: 10px;
    padding-bottom: 12px;
  }

  .auth-shell.is-register :deep(.auth-grid) {
    gap: 10px 16px;
  }

  .auth-shell.is-register :deep(.mc-input-group),
  .auth-shell.is-register :deep(.mc-select-group) {
    gap: 5px;
  }

  .auth-shell.is-register :deep(.mc-input),
  .auth-shell.is-register :deep(.mc-select-trigger),
  .auth-shell.is-register :deep(.captcha-box) {
    height: 44px;
  }

  .auth-shell.is-register :deep(.mc-button--primary) {
    min-height: 46px;
  }

  .auth-shell.is-register :deep(.auth-form-note) {
    line-height: 1.45;
  }
}

.auth-shell__panel-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--mc-hairline);
  color: var(--mc-slate);
  font-family: "JetBrains Mono", "Cascadia Mono", monospace;
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.auth-shell__panel-meta span:first-child {
  color: var(--mc-signal);
  font-weight: 700;
}

.auth-shell__panel-header {
  margin-top: 30px;
}

.auth-shell__panel-header h2 {
  margin: 0;
  font-size: clamp(2.4rem, 4vw, 4rem);
  font-weight: 650;
  line-height: 1;
  letter-spacing: -0.065em;
}

.auth-shell__panel-header p {
  max-width: 36rem;
  margin: 16px 0 0;
  color: var(--mc-slate);
  font-size: 14px;
  line-height: 1.75;
  text-wrap: pretty;
}

.auth-shell__panel-body {
  margin-top: 34px;
}

.auth-shell__footer {
  margin: 26px 0 0;
  color: var(--mc-slate);
  font-size: 13px;
  text-align: center;
}

.auth-shell__footer a {
  margin-left: 8px;
  color: var(--mc-ink);
  font-weight: 700;
  text-decoration: none;
  border-bottom: 1px solid rgba(20, 20, 19, 0.28);
}

.auth-shell__footer a:hover {
  color: var(--mc-signal);
  border-color: var(--mc-signal);
}

.auth-shell__workspace-note {
  position: absolute;
  right: 32px;
  bottom: 28px;
  margin: 0;
  color: rgba(20, 20, 19, 0.28);
  font-family: "JetBrains Mono", "Cascadia Mono", monospace;
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

:deep(.auth-form) {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

:deep(.auth-fieldset) {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--mc-hairline);
}

:deep(.auth-section-header) {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

:deep(.auth-section-label) {
  color: var(--mc-ink);
  font-size: 14px;
  font-weight: 700;
}

:deep(.auth-section-label::before) {
  content: '';
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 10px;
  border-radius: 50%;
  background: var(--mc-signal);
  vertical-align: 1px;
}

:deep(.auth-section-note),
:deep(.auth-form-note) {
  margin: 0;
  color: var(--mc-slate);
  font-size: 12px;
  line-height: 1.6;
}

:deep(.auth-grid) {
  display: grid;
  gap: 16px 20px;
}

:deep(.auth-grid--two) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

:deep(.captcha-row) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 142px;
  gap: 14px;
  align-items: end;
}

:deep(.captcha-input) {
  min-width: 0;
}

:deep(.captcha-box) {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  overflow: hidden;
  border: 1px solid rgba(20, 20, 19, 0.18);
  border-radius: 4px;
  background: var(--mc-lifted);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

:deep(.captcha-box:hover) {
  border-color: var(--mc-signal);
  transform: translateY(-1px);
}

:deep(.captcha-box::after) {
  content: '点击刷新';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(20, 20, 19, 0.8);
  color: var(--mc-white);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0;
  transition: opacity 0.2s ease;
}

:deep(.captcha-box:hover::after) {
  opacity: 1;
}

:deep(.captcha-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

:deep(.captcha-dummy) {
  color: var(--mc-slate);
  font-size: 12px;
}

:deep(.submit-btn) {
  width: 100%;
  margin-top: 2px;
}

:deep(.mc-input-group),
:deep(.mc-select-group) {
  gap: 8px;
}

:deep(.mc-label) {
  margin-left: 0;
  color: var(--mc-charcoal);
  font-size: 13px;
  font-weight: 650;
}

:deep(.mc-input),
:deep(.mc-select-trigger) {
  height: 50px;
  padding-inline: 16px;
  border: 1px solid rgba(20, 20, 19, 0.16);
  border-radius: 4px;
  background: rgba(252, 251, 250, 0.76);
  font-size: 14px;
}

:deep(.mc-select-trigger) {
  padding-right: 44px;
}

:deep(.mc-input:hover),
:deep(.mc-select-trigger:hover:not(.is-disabled)) {
  border-color: rgba(20, 20, 19, 0.4);
}

:deep(.mc-input:focus),
:deep(.mc-select-trigger.is-open) {
  border-color: var(--mc-signal);
  background: var(--mc-white);
  box-shadow: 0 0 0 3px rgba(207, 69, 0, 0.1);
}

:deep(.mc-select-chevron) {
  right: 16px;
}

:deep(.mc-button--primary) {
  min-height: 52px;
  border: 1px solid var(--mc-ink);
  border-radius: 4px;
  background: var(--mc-ink);
  color: var(--mc-white);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  box-shadow: 8px 8px 0 rgba(207, 69, 0, 0.18);
}

:deep(.mc-button--primary:hover:not(:disabled)) {
  transform: translate(-2px, -2px);
  box-shadow: 10px 10px 0 rgba(207, 69, 0, 0.24);
}

:deep(.mc-button--primary:active:not(:disabled)) {
  transform: translate(1px, 1px);
  box-shadow: 5px 5px 0 rgba(207, 69, 0, 0.2);
}

@keyframes routeIn {
  from {
    transform: scaleX(0);
  }
}

@keyframes routeInVertical {
  from {
    transform: scaleY(0);
  }
}

@keyframes stepIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes formIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1180px) {
  .auth-shell,
  .auth-shell.is-register {
    --story-width: minmax(340px, 38%);
  }

  .auth-shell__story {
    padding: 34px;
  }

  .auth-shell__title,
  .auth-shell.is-register .auth-shell__title {
    font-size: clamp(3rem, 5vw, 4.7rem);
  }

  .auth-shell__workspace {
    padding: 64px 48px;
  }

  .auth-shell__steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 24px;
  }

  .auth-shell.is-login .auth-shell__route {
    display: none;
  }
}

@media (max-width: 860px) {
  .auth-shell,
  .auth-shell.is-register {
    grid-template-columns: 1fr;
  }

  .auth-shell__story {
    min-height: auto;
    gap: 28px;
    padding: 28px 24px 32px;
  }

  .auth-shell__story::after {
    font-size: 15rem;
  }

  .auth-shell__title,
  .auth-shell.is-register .auth-shell__title {
    max-width: 12ch;
    font-size: clamp(2.8rem, 10vw, 4.6rem);
  }

  .auth-shell__subtitle {
    margin-top: 18px;
  }

  .auth-shell__dispatch {
    align-self: auto;
  }

  .auth-shell.is-register .auth-shell__steps {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .auth-shell.is-register .auth-shell__step {
    display: block;
  }

  .auth-shell.is-register .auth-shell__route {
    display: none;
  }

  .auth-shell__story-footer {
    display: none;
  }

  .auth-shell__workspace {
    min-height: auto;
    padding: 54px 24px 72px;
  }
}

@media (max-width: 620px) {
  .auth-shell__brand-code {
    display: none;
  }

  .auth-shell__steps,
  .auth-shell.is-register .auth-shell__steps {
    grid-template-columns: 1fr;
  }

  .auth-shell__step,
  .auth-shell.is-register .auth-shell__step {
    display: grid;
    grid-template-columns: 22px 1fr;
    gap: 14px;
  }

  .auth-shell__step-dot {
    margin: 0;
  }

  .auth-shell__step-copy {
    margin-top: -2px;
  }

  .auth-shell__panel-meta span:last-child,
  .auth-shell__workspace-note {
    display: none;
  }

  :deep(.auth-grid--two),
  :deep(.captcha-row) {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .auth-shell__route-signal,
  .auth-shell__step,
  .auth-shell__form-stage {
    animation: none;
    opacity: 1;
  }
}
</style>
