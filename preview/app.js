/* Screen Time Reminder — interactive preview.
   Screen time figures are simulated; the session/break logic is the same
   shape as the Android app. Settings are stored in localStorage. */

(() => {
  "use strict";

  const KEY = "justsimpleapps.screentime.preview.v2";
  const DEMO_HOUR = 20;
  const DEMO_CLOCK = "8:42";
  const BREAK_SECONDS = 5 * 60;
  const CIRCUMFERENCE = 2 * Math.PI * 94;

  const HOURS = [0, 0, 0, 0, 0, 1, 3, 8, 13, 10, 8, 11, 7, 5, 8, 12, 10, 7, 11, 20, 30, 0, 0, 0];
  const HIST_MIN = [198, 176, 214, 189, 243, 205];
  const HIST_BREAKS = [3, 2, 3, 2, 4, 2];
  const APPS = [
    ["Instagram", 48],
    ["Chrome", 36],
    ["YouTube", 29],
    ["Messages", 24],
    ["Gmail", 11]
  ];

  const state = {
    tab: "today",
    today: 2 * 3600 + 44 * 60 + 36,
    continuous: 44 * 60 + 15,
    sessionLimit: 45,
    dailyLimit: 300,
    breaks: 2,
    unlocks: 42,
    firstUse: "6:40 AM",
    reminders: true,
    repeat: true,
    vibrate: true,
    background: true,
    theme: "light",
    overlay: null,
    breakPhase: "idle",
    breakRemaining: BREAK_SECONDS,
    breakEndsAt: 0,
    breakSessionSeconds: 0,
    snoozeUntil: 0,
    overlayDismissed: false,
    onboardAccess: false,
    notificationTimer: null
  };

  const $ = (sel) => document.querySelector(sel);
  const pad = (n) => String(n).padStart(2, "0");

  const fmtHMS = (seconds) => {
    const s = Math.max(0, Math.floor(seconds));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return h > 0 ? `${h}:${pad(m)}:${pad(sec)}` : `${m}:${pad(sec)}`;
  };

  const fmtTimer = (seconds) => {
    const s = Math.max(0, Math.floor(seconds));
    return `${pad(Math.floor(s / 3600))}:${pad(Math.floor((s % 3600) / 60))}:${pad(s % 60)}`;
  };

  const fmtHM = (seconds) => {
    const total = Math.max(0, Math.floor(seconds / 60));
    const h = Math.floor(total / 60);
    const m = total % 60;
    if (h > 0 && m > 0) return `${h}h ${m}m`;
    if (h > 0) return `${h}h`;
    return `${m}m`;
  };

  const fmtChart = (seconds) => {
    const total = Math.max(0, Math.floor(seconds / 60));
    return `${Math.floor(total / 60)}:${pad(total % 60)}`;
  };

  const dateLabel = () => {
    const d = new Date();
    const weekday = d.toLocaleDateString("en-US", { weekday: "short" });
    const month = d.toLocaleDateString("en-US", { month: "long" });
    return `${weekday}, ${d.getDate()} ${month}`.toUpperCase();
  };

  const shortDay = (date) =>
    date.toLocaleDateString("en-US", { weekday: "short" }).toUpperCase();

  const MARK = `<svg class="mark" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9.4" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M12 12V6.6M12 12l3.8 2.4" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="12" cy="12" r="1" fill="currentColor"/></svg>`;
  const CHEV = `<svg class="set-chev" viewBox="0 0 12 12" aria-hidden="true"><path d="M4.4 2.4L8 6l-3.6 3.6" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>`;

  // ---------- persistence ----------

  function load() {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return;
      const saved = JSON.parse(raw);
      if (typeof saved.sessionLimit === "number") state.sessionLimit = saved.sessionLimit;
      if (typeof saved.dailyLimit === "number") state.dailyLimit = saved.dailyLimit;
      if (typeof saved.reminders === "boolean") state.reminders = saved.reminders;
      if (typeof saved.repeat === "boolean") state.repeat = saved.repeat;
      if (typeof saved.vibrate === "boolean") state.vibrate = saved.vibrate;
      if (typeof saved.background === "boolean") state.background = saved.background;
      if (["light", "dark", "system"].includes(saved.theme)) state.theme = saved.theme;
    } catch (_) {
      /* a fresh start is fine */
    }
  }

  function save() {
    try {
      localStorage.setItem(
        KEY,
        JSON.stringify({
          sessionLimit: state.sessionLimit,
          dailyLimit: state.dailyLimit,
          reminders: state.reminders,
          repeat: state.repeat,
          vibrate: state.vibrate,
          background: state.background,
          theme: state.theme
        })
      );
    } catch (_) {
      /* private mode, no matter */
    }
  }

  // ---------- derived ----------

  function sessionTarget() {
    return state.sessionLimit * 60;
  }

  function nextNudgeSeconds() {
    if (state.snoozeUntil && Date.now() < state.snoozeUntil) {
      return Math.ceil((state.snoozeUntil - Date.now()) / 1000);
    }
    return Math.max(0, sessionTarget() - state.continuous);
  }

  function isDue() {
    if (!state.reminders) return false;
    if (state.snoozeUntil && Date.now() < state.snoozeUntil) return false;
    return state.continuous >= sessionTarget();
  }

  function statusText() {
    if (!state.reminders || !state.background) return "MONITOR PAUSED";
    return "LIVE";
  }

  function statusLive() {
    return state.reminders && state.background;
  }

  function progressInfo() {
    const daily = state.dailyLimit > 0;
    const over = daily && state.today >= state.dailyLimit * 60;
    const pct = daily
      ? Math.min(100, (state.today / (state.dailyLimit * 60)) * 100)
      : Math.min(100, (state.continuous / sessionTarget()) * 100);
    const label = over
      ? "LIMIT REACHED"
      : daily
        ? `${fmtHM(state.dailyLimit * 60)} DAILY LIMIT`.toUpperCase()
        : `${state.sessionLimit}-MINUTE SESSIONS`;
    return { pct, over, label, daily };
  }

  function heroSub() {
    const { over, daily } = progressInfo();
    if (daily && over) return "Past your daily limit. The rest of the day is yours.";
    if (state.breaks === 0) return "Since midnight. No breaks counted yet.";
    return "Since midnight, across every app.";
  }

  function weekData() {
    const points = [];
    for (let i = 6; i >= 1; i -= 1) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      points.push({
        date: d,
        minutes: HIST_MIN[6 - i],
        breaks: HIST_BREAKS[6 - i],
        today: false
      });
    }
    points.push({
      date: new Date(),
      minutes: Math.floor(state.today / 60),
      breaks: state.breaks,
      today: true
    });
    return points;
  }

  // ---------- render: shared ----------

  function topbar() {
    return `<header class="topbar">${MARK}<span class="brand">SCREEN TIME REMINDER</span>
      <span class="topbar-right ${statusLive() ? "live" : ""}"><i></i>${statusText()}</span>
    </header>`;
  }

  function statsHTML() {
    return `<div class="stats">
      <div class="stat stat-small"><span class="mono">${state.firstUse}</span><span class="label">FIRST UNLOCK</span></div>
      <div class="stat-rule"></div>
      <div class="stat"><span class="mono">${state.unlocks}</span><span class="label">UNLOCKS</span></div>
      <div class="stat-rule"></div>
      <div class="stat"><span class="mono" id="stat-breaks">${state.breaks}</span><span class="label">BREAKS</span></div>
    </div>`;
  }

  function rhythmHTML() {
    const max = Math.max(...HOURS);
    const bars = HOURS.map((value, hour) => {
      const height = value > 0 ? Math.max(3, (value / max) * 100) : 2;
      const cls = hour === DEMO_HOUR ? "now" : value > 0 ? "has" : "";
      return `<span class="bar ${cls}" style="height:${height}%"></span>`;
    }).join("");
    return `<div class="rhythm">${bars}</div>
      <div class="axis"><span>00</span><span>06</span><span>12</span><span>18</span><span>24</span></div>`;
  }

  function appRowsHTML() {
    const top = APPS[0][1];
    return APPS.map(
      ([name, minutes], index) => `<div class="app-row">
        <div class="app-row-top">
          <span class="app-rank">${pad(index + 1)}</span>
          <span class="app-name">${name}</span>
          <span class="app-time">${fmtHM(minutes * 60)}</span>
        </div>
        <div class="app-bar"><i style="width:${Math.round((minutes / top) * 100)}%"></i></div>
      </div>`
    ).join("");
  }

  function breakCardHTML() {
    const due = isDue();
    return `<section class="break-card">
      <div class="break-card-top">
        <span class="label" style="color:var(--ink)">STEP AWAY</span>
        <span class="label" style="font-size:8px">${state.breaks} TODAY</span>
      </div>
      <h3 class="break-title">${due ? "Time to look up." : "Look up for a minute."}</h3>
      <p class="break-copy">Stand, stretch, and look at something far away. Twenty seconds of distance does more than twenty minutes of scrolling.</p>
      <button class="btn ghost" data-action="open-break">${due ? "TAKE A 5-MIN BREAK" : "START A 5-MIN BREAK"}</button>
    </section>`;
  }

  function promiseHTML() {
    return `<div class="promise">
      <div class="label">NO ADS · NO TRACKING · NO ACCOUNT</div>
      <a href="https://www.justsimpleapps.online/" target="_blank" rel="noopener">justsimpleapps.online</a>
    </div>`;
  }

  // ---------- render: today ----------

  function todayHTML() {
    const { pct, over, label } = progressInfo();
    const due = isDue() && !state.snoozeUntil;
    return `${topbar()}
      <div class="content">
        <div class="eyebrow-row">
          <span class="label">${dateLabel()}</span>
          <span class="label">THIS PHONE</span>
        </div>
        <div class="title-row">
          <h1 class="headline">Today</h1>
          ${over ? '<span class="label" style="color:var(--ink)">LIMIT REACHED</span>' : ""}
        </div>
        <p class="content-sub">A live count of the time you spend looking.</p>

        <section class="hero">
          <div class="hero-top">
            <span class="label">TIME ON SCREEN TODAY</span>
            <span class="hero-live"><i></i>LIVE</span>
          </div>
          <div class="hero-number" id="today-total">${fmtHMS(state.today)}</div>
          <p class="hero-sub" id="hero-sub">${heroSub()}</p>
          <div class="hero-rule"></div>
          <div class="hero-grid">
            <div>
              <div class="label">CONTINUOUS</div>
              <span class="mono" id="continuous">${fmtTimer(state.continuous)}</span>
            </div>
            <div>
              <div class="label" id="next-label">${state.reminders ? (due ? "STEP AWAY" : "NEXT NUDGE IN") : "REMINDERS"}</div>
              <span class="mono" id="next-nudge">${
                state.reminders ? (due ? "NOW" : fmtTimer(nextNudgeSeconds())) : "PAUSED"
              }</span>
            </div>
          </div>
          <div class="progress"><i id="hero-progress" style="width:${pct}%"></i></div>
          <div class="progress-meta">
            <span class="label" id="progress-label">${label}</span>
            <span class="label" id="progress-percent">${Math.round(pct)}%</span>
          </div>
          ${
            due
              ? '<div class="hero-action"><button class="btn ghost" data-action="open-break">TAKE A BREAK NOW</button></div>'
              : ""
          }
        </section>

        ${statsHTML()}

        <section class="section">
          <div class="section-head"><span class="label">YOUR RHYTHM</span><span class="label">24 HOURS</span></div>
          ${rhythmHTML()}
          <p class="section-note">Screen-on minutes by hour, since midnight.</p>
        </section>

        <section class="section">
          <div class="section-head"><span class="label">TOP APPS</span><span class="label">TODAY</span></div>
          <div class="app-list">${appRowsHTML()}</div>
        </section>

        ${breakCardHTML()}
        ${promiseHTML()}
      </div>`;
  }

  // ---------- render: week ----------

  function weekHTML() {
    const points = weekData();
    const max = Math.max(...points.map((p) => p.minutes), 1);
    const avg = Math.round(points.reduce((sum, p) => sum + p.minutes, 0) / points.length);
    const lightest = points.reduce((a, b) => (a.minutes <= b.minutes ? a : b));
    const weekBreaks = points.reduce((sum, p) => sum + p.breaks, 0);
    const diff = Math.floor(state.today / 60) - avg;
    const pace =
      Math.abs(diff) < 5
        ? "About average"
        : `${Math.round((Math.abs(diff) / avg) * 100)}% ${diff < 0 ? "below" : "above"} average`;

    const chart = points
      .map(
        (p, index) => `<div class="week-col ${p.today ? "today" : ""}">
          <span class="week-val" ${p.today ? 'id="week-today-val"' : ""}>${fmtChart(p.minutes * 60)}</span>
          <div class="week-space">
            <div class="week-bar" ${p.today ? 'id="week-today-bar"' : `data-bar="${index}"`}
                 style="height:${Math.max(2, Math.round((p.minutes / max) * 100))}%"></div>
          </div>
          <span class="week-day">${shortDay(p.date)}</span>
        </div>`
      )
      .join("");

    const rows = [...points]
      .reverse()
      .map(
        (p, i) => `<div class="day-row ${p.today ? "today" : ""}">
          <div class="day-row-top">
            <span class="label" style="color:${p.today ? "var(--ink)" : "var(--muted)"}">${shortDay(p.date)}</span>
            <span class="mono">${pad(p.date.getDate())}</span>
            <span class="day-row-time" ${p.today ? 'id="week-today-row-time"' : ""}>${p.minutes > 0 ? fmtHM(p.minutes * 60) : "—"}</span>
            ${p.today ? '<span class="today-tag">TODAY</span>' : ""}
          </div>
          <div class="day-track"><i style="width:${Math.max(2, Math.round((p.minutes / max) * 100))}%"></i></div>
        </div>`
      )
      .join("");

    return `${topbar()}
      <div class="content">
        <div class="eyebrow-row"><span class="label">YOUR WEEK</span><span class="label">LAST 7 DAYS</span></div>
        <h1 class="headline">Seven days.</h1>
        <p class="content-sub">The same record, looked at from further back.</p>

        <div class="week-summary" style="margin-top:24px">
          <div>
            <div class="label">DAILY AVERAGE</div>
            <div class="big mono" id="week-avg">${fmtHM(avg * 60)}</div>
          </div>
          <div style="text-align:right">
            <div class="label">DAYS TRACKED</div>
            <div class="big mono" style="font-size:16px">7 / 7</div>
          </div>
        </div>

        <div class="week-chart">${chart}</div>

        <section class="section">
          <div class="section-head"><span class="label">DAY BY DAY</span></div>
          ${rows}
        </section>

        <section class="section">
          <div class="section-head"><span class="label">PATTERNS</span></div>
          <div class="pattern-row"><span class="label">LIGHTEST DAY</span><span class="mono">${shortDay(lightest.date)} · ${fmtHM(lightest.minutes * 60)}</span></div>
          <div class="pattern-row"><span class="label">LONGEST SESSION</span><span class="mono">1h 52m</span></div>
          <div class="pattern-row"><span class="label">BREAKS THIS WEEK</span><span class="mono">${weekBreaks}</span></div>
          <div class="pattern-row"><span class="label">TODAY VS AVERAGE</span><span class="mono">${pace}</span></div>
        </section>

        <p class="foot-note">History begins the first day the app runs. Android does not share usage from before an app is installed.</p>
        ${promiseHTML()}
      </div>`;
  }

  // ---------- render: settings ----------

  function segmented(action, options, selected) {
    return `<div class="segmented">${options
      .map(
        ([value, label]) =>
          `<button data-action="${action}" data-value="${value}" class="${selected === value ? "on" : ""}">${label}</button>`
      )
      .join("")}</div>`;
  }

  function switchRow(key, title, sub) {
    return `<div class="set-row">
      <div class="set-row-main">
        <div class="set-title">${title}</div>
        ${sub ? `<div class="set-sub">${sub}</div>` : ""}
      </div>
      <button class="switch ${state[key] ? "on" : ""}" data-action="toggle" data-key="${key}" aria-label="${title}" aria-pressed="${state[key]}"></button>
    </div>`;
  }

  function settingsHTML() {
    const todayMin = Math.floor(state.today / 60);
    return `${topbar()}
      <div class="content">
        <div class="eyebrow-row"><span class="label">PREFERENCES</span><span class="label">1.0.0</span></div>
        <h1 class="headline">Settings</h1>
        <p class="content-sub">A few switches. Nothing buried, nothing extra.</p>

        <section class="settings-section">
          <span class="label">SESSIONS &amp; REMINDERS</span>
          ${switchRow("reminders", "Reminders", "A calm nudge when a screen session runs long.")}
          <div class="set-block">
            <div class="set-block-head">
              <span class="set-title">Reminder after</span>
              <span class="set-value">${state.sessionLimit} MIN</span>
            </div>
            ${segmented("set-session", [[30, "30M"], [45, "45M"], [60, "60M"], [90, "90M"]], state.sessionLimit)}
          </div>
          ${switchRow("repeat", "Repeat while still on screen", `Nudge again every ${state.sessionLimit} minutes if you have not stepped away.`)}
          ${switchRow("vibrate", "Vibrate on reminder", "Sound comes from your notification settings.")}
        </section>

        <section class="settings-section">
          <span class="label">DAILY LIMIT</span>
          <div class="set-block" style="border-bottom:1px solid var(--line)">
            <div class="set-block-head"><span class="set-title">Stop nudging me after</span></div>
            <div class="set-sub" style="margin-top:4px">A second, firmer reminder when the day's total passes this line.</div>
            ${segmented("set-daily", [[120, "2H"], [180, "3H"], [300, "5H"], [360, "6H"], [0, "OFF"]], state.dailyLimit)}
            <p class="set-hint" id="settings-today">TODAY ${fmtHM(todayMin * 60).toUpperCase()} OF ${state.dailyLimit > 0 ? fmtHM(state.dailyLimit * 60).toUpperCase() : "NO LIMIT"}</p>
          </div>
        </section>

        <section class="settings-section">
          <span class="label">MONITORING</span>
          ${switchRow("background", "Background monitoring", "Keeps watching while the app is closed. A quiet, ongoing notice stays in your shade.")}
        </section>

        <section class="settings-section">
          <span class="label">ACCESS</span>
          <div class="set-row">
            <div class="set-row-main">
              <div class="set-title">Screen time access</div>
              <div class="set-sub">The phone's own usage record. Read on this device only.</div>
            </div>
            <span class="set-value" style="color:var(--ink)">ALLOWED</span>
          </div>
          <div class="set-row">
            <div class="set-row-main">
              <div class="set-title">Notifications</div>
              <div class="set-sub">Reminders and the daily limit notice.</div>
            </div>
            <span class="set-value" style="color:var(--ink)">ON</span>
          </div>
        </section>

        <section class="settings-section">
          <span class="label">APPEARANCE</span>
          <div class="set-block" style="border-bottom:1px solid var(--line)">
            ${segmented("set-theme", [["light", "LIGHT"], ["dark", "DARK"], ["system", "SYSTEM"]], state.theme)}
            <p class="set-hint">Pure Light, Deep Dark, or follow the phone.</p>
          </div>
        </section>

        <section class="settings-section">
          <span class="label">TEST &amp; DATA</span>
          <div class="set-row clickable" data-action="test">
            <div class="set-row-main">
              <div class="set-title">Send a test reminder</div>
              <div class="set-sub">See exactly what a nudge looks like.</div>
            </div>
            <span class="set-value">SEND</span>${CHEV}
          </div>
          <div class="set-row clickable" data-action="reset-open">
            <div class="set-row-main">
              <div class="set-title">Reset today's tracking</div>
              <div class="set-sub">Start the day fresh inside the app.</div>
            </div>
            <span class="set-value">RESET</span>${CHEV}
          </div>
        </section>

        <section class="settings-section">
          <span class="label">ABOUT</span>
          <div class="about-brand">
            ${MARK}
            <div>
              <div class="name">Screen Time Reminder</div>
              <div class="ver">VERSION 1.0.0 · ANDROID</div>
            </div>
          </div>
          <p class="about-copy">Free to use. No ads, no tracking, no account. Screen-time numbers, settings and daily totals stay on this phone.</p>
          <a class="about-link" href="https://www.justsimpleapps.online/" target="_blank" rel="noopener">JUSTSIMPLEAPPS.ONLINE</a>
          <p class="foot-note" style="margin-top:26px">MADE FOR ONE THING · NOT FOR ATTENTION</p>
        </section>
      </div>`;
  }

  // ---------- render: shell ----------

  function renderNav() {
    const items = [
      ["today", "TODAY"],
      ["week", "WEEK"],
      ["settings", "SETTINGS"]
    ];
    $("#bottomnav").innerHTML = items
      .map(
        ([value, label]) =>
          `<button data-action="tab" data-value="${value}" class="${state.tab === value ? "active" : ""}">${label}<i></i></button>`
      )
      .join("");
  }

  function render(options = {}) {
    const main = $("#app-main");
    const scroll = options.keepScroll ? main.scrollTop : 0;
    if (state.tab === "today") main.innerHTML = todayHTML();
    else if (state.tab === "week") main.innerHTML = weekHTML();
    else main.innerHTML = settingsHTML();
    main.scrollTop = scroll;
    renderNav();
    applyTheme();
    updateLive();
  }

  function applyTheme() {
    const phone = $("#phone");
    const dark =
      state.theme === "dark" ||
      (state.theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);
    phone.dataset.theme = dark ? "dark" : "light";
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) {
      const mobile = window.matchMedia("(max-width: 900px)").matches;
      meta.setAttribute("content", mobile ? (dark ? "#0B0B0C" : "#FFFFFF") : "#F1EFEA");
    }
  }

  function updateLive() {
    const $id = (id) => document.getElementById(id);
    const total = $id("today-total");
    if (total) total.textContent = fmtHMS(state.today);

    const continuous = $id("continuous");
    if (continuous) continuous.textContent = fmtTimer(state.continuous);

    const next = $id("next-nudge");
    if (next) {
      next.textContent = !state.reminders
        ? "PAUSED"
        : isDue() && !state.snoozeUntil
          ? "NOW"
          : fmtTimer(nextNudgeSeconds());
    }

    const nextLabel = $id("next-label");
    if (nextLabel) {
      nextLabel.textContent = !state.reminders
        ? "REMINDERS"
        : isDue() && !state.snoozeUntil
          ? "STEP AWAY"
          : "NEXT NUDGE IN";
    }

    const { pct, label, over } = progressInfo();
    const bar = $id("hero-progress");
    if (bar) bar.style.width = `${pct}%`;
    const labelEl = $id("progress-label");
    if (labelEl) labelEl.textContent = label;
    const pctEl = $id("progress-percent");
    if (pctEl) pctEl.textContent = `${Math.round(pct)}%`;
    const sub = $id("hero-sub");
    if (sub) sub.textContent = heroSub();
    const breaks = $id("stat-breaks");
    if (breaks) breaks.textContent = String(state.breaks);

    const status = document.querySelector(".topbar-right");
    if (status) {
      status.classList.toggle("live", statusLive());
      status.innerHTML = `<i></i>${statusText()}`;
    }

    const todayLimit = $id("settings-today");
    if (todayLimit) {
      todayLimit.textContent = `TODAY ${fmtHM(state.today).toUpperCase()} OF ${
        state.dailyLimit > 0 ? fmtHM(state.dailyLimit * 60).toUpperCase() : "NO LIMIT"
      }`;
    }

    updateWeekLive();
  }

  function updateWeekLive() {
    const points = weekData();
    const max = Math.max(...points.map((p) => p.minutes), 1);
    const todayPoint = points[points.length - 1];
    const avg = Math.round(points.reduce((sum, p) => sum + p.minutes, 0) / points.length);

    const val = document.getElementById("week-today-val");
    if (val) val.textContent = fmtChart(todayPoint.minutes * 60);
    const bar = document.getElementById("week-today-bar");
    if (bar) bar.style.height = `${Math.max(2, Math.round((todayPoint.minutes / max) * 100))}%`;
    const rowTime = document.getElementById("week-today-row-time");
    if (rowTime) rowTime.textContent = fmtHM(todayPoint.minutes * 60);
    const avgEl = document.getElementById("week-avg");
    if (avgEl) avgEl.textContent = fmtHM(avg * 60);
  }

  // ---------- break overlay ----------

  function openBreakOverlay() {
    if (state.overlay === "break") return;
    $("#onboarding").hidden = true;
    state.overlay = "break";
    state.breakPhase = "idle";
    state.breakRemaining = BREAK_SECONDS;
    state.breakSessionSeconds = state.continuous;
    $("#break-overlay").hidden = false;
    $("#break-overlay").scrollTop = 0;
    renderBreakOverlay();
  }

  function closeBreakOverlay() {
    if (isDue()) state.overlayDismissed = true;
    state.overlay = null;
    state.breakPhase = "idle";
    state.breakRemaining = BREAK_SECONDS;
    $("#break-overlay").hidden = true;
    hideNotification();
    render({ keepScroll: true });
  }

  function startBreak() {
    state.breaks += 1;
    state.continuous = 0;
    state.snoozeUntil = 0;
    state.overlayDismissed = false;
    state.breakPhase = "running";
    state.breakRemaining = BREAK_SECONDS;
    state.breakEndsAt = Date.now() + BREAK_SECONDS * 1000;
    hideNotification();
    renderBreakOverlay();
  }

  function snooze() {
    state.snoozeUntil = Date.now() + 15 * 60 * 1000;
    state.overlayDismissed = true;
    hideNotification();
    if (state.overlay === "break") {
      state.overlay = null;
      state.breakPhase = "idle";
      $("#break-overlay").hidden = true;
    }
    render({ keepScroll: true });
  }

  function renderBreakOverlay() {
    const phase = state.breakPhase;
    const eyebrow = phase === "done" ? "BREAK COMPLETE" : "STEP AWAY";
    const title = phase === "done" ? "Welcome\nback." : "Look up.";
    let copy;
    if (phase === "done") {
      copy = "Five minutes for your eyes and your attention. That is the whole idea.";
    } else if (state.breakSessionSeconds > 60) {
      copy = `You have been on screen for ${fmtHM(state.breakSessionSeconds)}. Give your eyes five minutes of distance.`;
    } else {
      copy = "Stand up, stretch, and look at something far away. Five minutes is enough to reset.";
    }

    document.getElementById("break-eyebrow").textContent = eyebrow;
    document.getElementById("break-title").textContent = title;
    document.getElementById("break-copy").textContent = copy;

    const actions = document.getElementById("break-actions");
    if (phase === "idle") {
      actions.innerHTML = `<button class="btn solid" data-action="break-start">START A 5-MIN BREAK</button>
        <button class="btn ghost" data-action="snooze">SNOOZE 15 MINUTES</button>`;
    } else if (phase === "running") {
      actions.innerHTML = `<button class="btn solid" data-action="break-close">I'M BACK EARLY</button>
        <p class="ov-note">The break already counts. Leaving early is your call.</p>`;
    } else {
      actions.innerHTML = `<button class="btn solid" data-action="break-close">BACK TO TODAY</button>`;
    }

    document.getElementById("break-foot").textContent =
      `${state.breaks} BREAKS TODAY · NOTHING LEAVES THIS PHONE`;

    if (phase === "running") {
      updateDial(1 - state.breakRemaining / BREAK_SECONDS, fmtTimer(state.breakRemaining), "RESTING");
    } else if (phase === "done") {
      updateDial(1, "00:00", "DONE");
    } else {
      updateDial(0, "05:00", "5 MINUTES");
    }
  }

  function updateDial(progress, time, label) {
    const fg = document.getElementById("dial-fg");
    if (fg) fg.style.strokeDashoffset = String(CIRCUMFERENCE * (1 - Math.max(0, Math.min(1, progress))));
    const dialTime = document.getElementById("dial-time");
    if (dialTime) dialTime.textContent = time;
    const dialLabel = document.getElementById("dial-label");
    if (dialLabel) dialLabel.textContent = label;
  }

  function updateBreakCountdown() {
    if (state.breakPhase !== "running") return;
    const remaining = Math.max(0, Math.ceil((state.breakEndsAt - Date.now()) / 1000));
    state.breakRemaining = remaining;
    updateDial(1 - remaining / BREAK_SECONDS, fmtTimer(remaining), "RESTING");
    if (remaining <= 0) {
      state.breakPhase = "done";
      renderBreakOverlay();
    }
  }

  // ---------- onboarding ----------

  function openOnboarding() {
    $("#break-overlay").hidden = true;
    state.breakPhase = "idle";
    state.overlay = "onboarding";
    state.onboardAccess = false;
    $("#onboarding").hidden = false;
    $("#onboarding").scrollTop = 0;
    renderOnboarding();
  }

  function closeOnboarding() {
    state.overlay = null;
    $("#onboarding").hidden = true;
    render({ keepScroll: true });
  }

  function renderOnboarding() {
    const actions = document.getElementById("onboard-actions");
    if (state.onboardAccess) {
      actions.innerHTML = `
        <div class="granted-row"><i></i><span class="label">ACCESS GRANTED</span><span class="mono">READY</span></div>
        <button class="btn solid" data-action="onboard-close">CONTINUE</button>`;
    } else {
      actions.innerHTML = `
        <button class="btn solid" data-action="onboard-allow">ALLOW SCREEN TIME ACCESS</button>
        <button class="btn ghost" data-action="onboard-close" style="margin-top:10px">NOT NOW</button>
        <p class="ov-note">Android opens its settings. Find Screen Time Reminder in the list and allow usage access. You can change this later in Settings.</p>`;
    }
  }

  // ---------- system notification ----------

  function showNotification(kind = "break") {
    const n = $("#notification");
    const title = $("#notif-title");
    const body = $("#notif-body");
    const actions = $("#notif-actions");
    n.classList.toggle("test", kind === "test");

    if (kind === "test") {
      title.textContent = "This is what a nudge looks like";
      body.textContent = "You are all set. Reminders will arrive like this.";
      actions.innerHTML = "";
    } else if (kind === "info") {
      title.textContent = "Screen time access";
      body.textContent = "Allowed. Readings stay on this phone.";
      actions.innerHTML = "";
    } else {
      title.textContent = "Time to look up";
      body.textContent = `${fmtHM(state.continuous)} on screen. Take a few minutes away.`;
      actions.innerHTML = `<button data-action="open-break">START BREAK</button>
        <button data-action="snooze">SNOOZE 15 MIN</button>`;
    }
    n.classList.add("show");
    clearTimeout(state.notificationTimer);
    state.notificationTimer = setTimeout(hideNotification, kind === "break" ? 9000 : 6000);
  }

  function hideNotification() {
    const n = $("#notification");
    if (n) n.classList.remove("show");
    clearTimeout(state.notificationTimer);
  }

  // ---------- tick ----------

  let lastTick = Date.now();
  let lastDue = false;

  function tick() {
    const now = Date.now();
    const elapsed = Math.min(5, Math.max(0, Math.round((now - lastTick) / 1000)));
    lastTick = now;

    const onBreak = state.overlay === "break" && state.breakPhase === "running";
    if (!document.hidden && !onBreak) {
      state.today += elapsed;
      state.continuous += elapsed;
    }
    if (state.breakPhase === "running") updateBreakCountdown();

    if (state.snoozeUntil && now >= state.snoozeUntil) {
      state.snoozeUntil = 0;
      state.overlayDismissed = false;
    }

    if (
      !document.hidden &&
      state.overlay === null &&
      isDue() &&
      !state.overlayDismissed
    ) {
      openBreakOverlay();
      setTimeout(() => {
        if (state.overlay === "break" && !state.breakPhase !== "done") showNotification("break");
      }, 450);
    }

    updateLive();

    const dueNow = isDue();
    if (dueNow !== lastDue) {
      lastDue = dueNow;
      if (state.overlay === null) render({ keepScroll: true });
    }
  }

  // ---------- events ----------

  function closeOverlays() {
    if (state.overlay === "break") {
      $("#break-overlay").hidden = true;
      state.breakPhase = "idle";
      state.breakRemaining = BREAK_SECONDS;
    }
    if (state.overlay === "onboarding") $("#onboarding").hidden = true;
    state.overlay = null;
    hideNotification();
    $("#reset-dialog").hidden = true;
  }

  document.addEventListener("click", (event) => {
    const target = event.target.closest("[data-action]");
    if (!target) return;
    const action = target.dataset.action;
    const value = target.dataset.value;

    switch (action) {
      case "tab":
        closeOverlays();
        state.tab = value || "today";
        render();
        break;

      case "toggle": {
        const key = target.dataset.key;
        if (key in state) {
          state[key] = !state[key];
          if (key === "reminders" && !state.reminders) hideNotification();
          save();
          render({ keepScroll: true });
        }
        break;
      }

      case "set-session":
        state.sessionLimit = Number(value);
        state.snoozeUntil = 0;
        state.overlayDismissed = false;
        save();
        render({ keepScroll: true });
        break;

      case "set-daily":
        state.dailyLimit = Number(value);
        save();
        render({ keepScroll: true });
        break;

      case "set-theme":
        state.theme = value;
        save();
        applyTheme();
        render({ keepScroll: true });
        break;

      case "open-break":
        hideNotification();
        openBreakOverlay();
        break;

      case "break-close":
        closeBreakOverlay();
        break;

      case "break-start":
        startBreak();
        break;

      case "snooze":
        snooze();
        break;

      case "test":
        showNotification("test");
        break;

      case "reset-open":
        $("#reset-dialog").hidden = false;
        break;

      case "reset-cancel":
        $("#reset-dialog").hidden = true;
        break;

      case "reset-confirm":
        state.today = 0;
        state.continuous = 0;
        state.breaks = 0;
        state.snoozeUntil = 0;
        state.overlayDismissed = false;
        $("#reset-dialog").hidden = true;
        render({ keepScroll: true });
        break;

      case "open-onboarding":
        openOnboarding();
        break;

      case "onboard-allow":
        state.onboardAccess = true;
        renderOnboarding();
        break;

      case "onboard-close":
        closeOnboarding();
        break;

      default:
        break;
    }
  });

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    if (!$("#reset-dialog").hidden) {
      $("#reset-dialog").hidden = true;
      return;
    }
    if (state.overlay === "break") closeBreakOverlay();
    else if (state.overlay === "onboarding") closeOnboarding();
  });

  document.addEventListener("visibilitychange", () => {
    lastTick = Date.now();
  });

  const systemTheme = window.matchMedia("(prefers-color-scheme: dark)");
  if (systemTheme.addEventListener) {
    systemTheme.addEventListener("change", () => {
      if (state.theme === "system") {
        applyTheme();
        render({ keepScroll: true });
      }
    });
  }

  window.addEventListener("resize", applyTheme);

  // ---------- boot ----------

  load();
  // Always begin 45 seconds away from the next nudge, whatever the saved
  // session length is. It makes the core loop visible without waiting 45 min.
  state.continuous = Math.min(state.continuous, Math.max(5, sessionTarget() - 45));
  state.breakSessionSeconds = state.continuous;
  $("#status-time").textContent = DEMO_CLOCK;
  applyTheme();
  render();
  lastDue = isDue();
  setInterval(tick, 1000);
})();
