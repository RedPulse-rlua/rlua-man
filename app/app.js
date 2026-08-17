(function () {
  'use strict';

  const API = '';
  const $ = (id) => document.getElementById(id);
  const app = $('app');
  const bottomNav = $('bottomNav');
  const authModal = $('authModal');
  let me = null;
  let currentPage = 'lobby';

  /* ===== Helpers ===== */
  function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function daysPlural(n) {
    const m10 = n % 10, m100 = n % 100;
    if (m10 === 1 && m100 !== 11) return n + ' день';
    if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return n + ' дня';
    return n + ' дней';
  }

  function toast(msg) {
    const t = $('toast');
    t.textContent = msg;
    t.classList.remove('hidden');
    clearTimeout(t._timer);
    t._timer = setTimeout(() => t.classList.add('hidden'), 3000);
  }

  function showModal(tab) {
    authModal.classList.remove('hidden');
    document.querySelectorAll('.tab').forEach((t) => t.classList.toggle('active', t.dataset.tab === tab));
    $('loginForm').classList.toggle('hidden', tab !== 'login');
    $('registerForm').classList.toggle('hidden', tab !== 'register');
    hideErrors();
  }

  function closeModal() {
    authModal.classList.add('hidden');
    hideErrors();
  }

  function hideErrors() {
    ['loginError', 'registerError', 'registerOk'].forEach((id) => {
      const el = $(id);
      if (el) el.classList.add('hidden');
    });
  }

  async function api(path, opts = {}) {
    const stored = (() => { try { return JSON.parse(localStorage.getItem('cc_session') || 'null'); } catch { return null; } })();
    const headers = { 'Content-Type': 'application/json' };
    if (stored?.token) headers['Authorization'] = 'Bearer ' + stored.token;
    const res = await fetch(API + path, { ...opts, headers: { ...headers, ...opts.headers } });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || 'Ошибка сервера');
    return data;
  }

  function saveSession(username, token) {
    localStorage.setItem('cc_session', JSON.stringify({ username, token }));
  }

  /* ===== Auth ===== */
  async function refreshAuth() {
    try {
      const data = await api('/api/me');
      if (data.ok && data.user) { me = data.user; } else { me = null; localStorage.removeItem('cc_session'); }
    } catch { me = null; localStorage.removeItem('cc_session'); }

    const logged = Boolean(me);
    $('authModal').classList.add('hidden');

    bottomNav.style.display = logged ? '' : 'none';
    $('bnavAdmin').style.display = logged && me?.role === 'admin' ? '' : 'none';
    if (!logged) navigate('lobby');
  }

  /* ===== Router ===== */
  function navigate(page) {
    if (page === currentPage && app.innerHTML.trim()) return;
    currentPage = page;
    window.history.pushState({}, '', '/' + page);
    renderPage(page);
    bottomNav.querySelectorAll('.bnav-btn').forEach((b) => b.classList.toggle('active', b.dataset.page === page));
  }

  window.addEventListener('popstate', () => {
    const path = location.pathname.replace(/^\//, '').split('/')[0] || 'lobby';
    currentPage = '';
    navigate(path);
  });

  function renderPage(page) {
    if (!me && !['lobby'].includes(page)) { page = 'lobby'; currentPage = page; }
    switch (page) {
      case 'lobby': return renderLobby();
      case 'lua': return renderLua();
      case 'vizor': return renderVizor();
      case 'profile': return renderProfile();
      case 'admin': return renderAdmin();
      case 'obf': return renderObf();
      default: return renderLobby();
    }
  }

  /* ===== Pages ===== */

  /* --- LOBBY --- */
  function renderLobby() {
    if (!me) {
      app.innerHTML = `
        <div class="page">
          <div class="top-bar">
            <a href="#" class="logo" onclick="event.preventDefault()">rl<span>ua</span></a>
            <div class="top-actions">
              <button class="btn btn-ghost btn-sm" id="topLogin">Войти</button>
              <button class="btn btn-primary btn-sm" id="topReg">Аккаунт</button>
            </div>
          </div>
          <div class="container hero">
            <span class="hero-badge">// rlua v1.0</span>
            <h1 class="hero-title">Защити свой <span>Lua-код</span></h1>
            <p class="hero-sub">Профессиональная обфускация Lua для Roblox</p>
            <div class="hero-actions">
              <button class="btn btn-primary" onclick="window._showReg()">Начать</button>
              <button class="btn btn-outline" onclick="window._showLogin()">Войти</button>
            </div>
          </div>
          <div class="container features-grid">
            <div class="feature-card">
              <div class="feature-icon">🛡</div>
              <h3>Многоуровневая защита</h3>
              <p>Обфускация в несколько проходов, защита от декомпиляции</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon">⚡</div>
              <h3>Мгновенная обработка</h3>
              <p>Защита кода за считанные секунды</p>
            </div>
            <div class="feature-card">
              <div class="feature-icon">👤</div>
              <h3>Личный кабинет</h3>
              <p>Аккаунт с историей и статистикой</p>
            </div>
          </div>
        </div>`;
      $('topLogin')?.addEventListener('click', () => showModal('login'));
      $('topReg')?.addEventListener('click', () => showModal('register'));
    } else {
      const days = me.createdAt ? Math.max(0, Math.floor((Date.now() - me.createdAt) / 86400000)) : 0;
      const dateStr = me.createdAt ? new Date(me.createdAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }) : '';
      app.innerHTML = `
        <div class="page">
          <div class="top-bar">
            <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
            <div class="top-actions">
              <span style="font-size:13px;color:var(--muted)">${esc(me.username)}</span>
              <button class="btn btn-ghost btn-sm" id="logoutBtn">Выйти</button>
            </div>
          </div>
          <div class="container">
            <div class="lobby-head">
              <div class="avatar">${esc(me.username.slice(0, 2).toUpperCase())}</div>
              <div>
                <div class="lobby-name">${esc(me.username)}</div>
                ${me.role === 'admin' ? '<div class="lobby-role">ADMIN</div>' : ''}
                <div class="lobby-date">на rlua с ${esc(dateStr)}</div>
              </div>
            </div>
            <div class="stats-grid">
              <div class="stat-card"><div class="stat-label">ID</div><div class="stat-value">#${me.id || '--'}</div></div>
              <div class="stat-card"><div class="stat-label">Роль</div><div class="stat-value">${me.role === 'admin' ? 'Админ' : 'Юзер'}</div></div>
              <div class="stat-card"><div class="stat-label">На сайте</div><div class="stat-value">${days === 0 ? 'Сегодня' : daysPlural(days)}</div></div>
              <div class="stat-card"><div class="stat-label">Скриптов</div><div class="stat-value">${me.scripts || 0}</div></div>
            </div>
          </div>
        </div>`;
      $('logoutBtn')?.addEventListener('click', async () => {
        try { await api('/api/logout', { method: 'POST' }); } catch {}
        localStorage.removeItem('cc_session');
        me = null;
        navigate('lobby');
      });
    }
  }

  /* --- LUA (VFS File Editor) --- */
  function renderLua() {
    app.innerHTML = `
      <div class="page">
        <div class="top-bar">
          <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
          <div class="top-actions">
            <button class="btn btn-ghost btn-sm" id="luaRefresh">⟳</button>
          </div>
        </div>
        <div class="container">
          <div class="section-header">
            <div class="section-title">RLUA — Файлы</div>
          </div>
          ${!me?.trusted ? '<div style="font-size:12px;color:var(--muted);margin-bottom:8px">Только чтение. Запись — доверенным.</div>' : ''}
          <input class="search-bar" id="luaSearch" placeholder="Поиск файлов…">
          <div id="luaTree"></div>
        </div>
        <div class="editor-panel" id="editorPanel">
          <div class="editor-topbar">
            <button class="back-btn" id="editorBack">←</button>
            <div class="editor-filename" id="editorName">—</div>
            <div class="editor-actions">
              <button class="btn btn-ghost btn-sm" id="editorSave" style="display:${me?.trusted ? '' : 'none'}">Сохранить</button>
            </div>
          </div>
          <div class="editor-body">
            <textarea class="code-editor" id="editorArea" spellcheck="false"></textarea>
          </div>
        </div>
      </div>
      <div class="wait-overlay hidden" id="obfWait"><div class="spinner"></div><div class="wait-msg" id="obfWaitMsg">Загрузка…</div></div>`;

    let tree = [];
    let currentItemId = null;

    async function loadTree() {
      try {
        const data = await api('/api/fs');
        tree = data.items || [];
        renderTree();
      } catch (e) { toast(e.message); }
    }

    function renderTree(filter = '') {
      const el = $('luaTree');
      if (!tree.length) { el.innerHTML = '<div class="empty-state"><div class="icon">📁</div><p>Хранилище пустое</p></div>'; return; }
      const q = filter.toLowerCase();
      let html = '';
      const walk = (items, depth = 0) => {
        for (const item of items) {
          if (q && !item.name.toLowerCase().includes(q)) continue;
          const isDir = item.type === 'folder';
          const indent = depth * 16;
          const icon = isDir ? '📁' : (item.name.endsWith('.lua') ? '📜' : item.name.endsWith('.py') ? '🐍' : '📄');
          html += `<button class="tree-item${currentItemId === item.id ? ' active' : ''}" data-id="${item.id}" data-type="${item.type}" style="padding-left:${14 + indent}px">
            <span class="icon">${icon}</span>
            <span class="name">${esc(item.name)}</span>
            ${item.size ? '<span class="meta">' + (item.size > 1024 ? Math.round(item.size / 1024) + 'K' : item.size + 'B') + '</span>' : ''}
          </button>`;
          if (isDir && item.children) walk(item.children, depth + 1);
        }
      };
      walk(tree);
      el.innerHTML = html || '<div class="empty-state"><p>Ничего не найдено</p></div>';

      el.querySelectorAll('.tree-item').forEach((btn) => {
        btn.addEventListener('click', async () => {
          if (btn.dataset.type === 'folder') return;
          try {
            const data = await api('/api/fs/item/' + btn.dataset.id);
            currentItemId = btn.dataset.id;
            $('editorName').textContent = data.name || '—';
            $('editorArea').value = data.content || '';
            $('editorPanel').classList.add('open');
            renderTree($('luaSearch')?.value || '');
          } catch (e) { toast(e.message); }
        });
      });
    }

    $('editorBack')?.addEventListener('click', () => $('editorPanel').classList.remove('open'));
    $('editorSave')?.addEventListener('click', async () => {
      if (!currentItemId) return;
      try {
        await api('/api/fs/item/' + currentItemId, { method: 'POST', body: JSON.stringify({ content: $('editorArea').value }) });
        toast('Сохранено');
      } catch (e) { toast(e.message); }
    });
    $('luaSearch')?.addEventListener('input', (e) => renderTree(e.target.value));
    $('luaRefresh')?.addEventListener('click', loadTree);

    loadTree();
  }

  /* --- VIZOR --- */
  function renderVizor() {
    app.innerHTML = `
      <div class="page">
        <div class="top-bar">
          <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
          <div class="top-actions"></div>
        </div>
        <div class="container">
          <div class="section-header"><div class="section-title">VIZOR</div></div>
          <div id="vizorContent"></div>
        </div>
      </div>`;

    const vc = $('vizorContent');

    async function checkRoom() {
      try {
        const data = await api('/api/vizor/me');
        if (data.room) { renderRoom(data.room); return; }
      } catch {}
      renderLanding();
    }

    function renderLanding() {
      vc.innerHTML = `
        <div class="vizor-grid">
          <div class="vizor-card">
            <h3>Создать комнату</h3>
            <input class="vizor-input" id="vCreateName" maxlength="40" placeholder="Название" style="margin-bottom:10px">
            <input class="vizor-input" id="vCreateLimit" type="number" min="1" max="50" value="10" style="margin-bottom:10px">
            <button class="btn btn-primary btn-block" id="vBtnCreate">Создать</button>
            <p class="vizor-err hidden" id="vCreateErr"></p>
          </div>
          <div class="vizor-card">
            <h3>Войти по коду</h3>
            <div style="display:flex;gap:8px">
              <input class="vizor-input code" id="vJoinCode" maxlength="6" placeholder="КОД" style="flex:1">
              <button class="btn btn-primary" id="vBtnJoin">Войти</button>
            </div>
            <p class="vizor-err hidden" id="vJoinErr"></p>
          </div>
        </div>`;

      $('vBtnCreate')?.addEventListener('click', async () => {
        const name = $('vCreateName')?.value?.trim();
        const limit = parseInt($('vCreateLimit')?.value) || 10;
        if (!name) { $('vCreateErr').textContent = 'Введите название'; $('vCreateErr').classList.remove('hidden'); return; }
        try {
          const data = await api('/api/vizor/create', { method: 'POST', body: JSON.stringify({ name, limit }) });
          if (data.room) renderRoom(data.room);
        } catch (e) { $('vCreateErr').textContent = e.message; $('vCreateErr').classList.remove('hidden'); }
      });

      $('vBtnJoin')?.addEventListener('click', async () => {
        const code = $('vJoinCode')?.value?.trim()?.toUpperCase();
        if (!code) { $('vJoinErr').textContent = 'Введите код'; $('vJoinErr').classList.remove('hidden'); return; }
        try {
          const data = await api('/api/vizor/join', { method: 'POST', body: JSON.stringify({ code }) });
          if (data.room) renderRoom(data.room);
        } catch (e) { $('vJoinErr').textContent = e.message; $('vJoinErr').classList.remove('hidden'); }
      });
    }

    function renderRoom(room) {
      const members = room.members || [];
      vc.innerHTML = `
        <div class="room-header">
          <div>
            <div style="font-weight:700;font-size:16px">${esc(room.name || 'Комната')}</div>
            <div class="room-code" id="vRoomCode" title="Нажми чтобы скопировать">${esc(room.code || '------')}</div>
          </div>
          <button class="btn btn-ghost btn-sm" id="vBtnLeave">Покинуть</button>
        </div>
        <div class="members-grid">
          ${members.map((m) => `
            <div class="member-card">
              <div class="member-avatar">${esc((m.username || '?').slice(0, 2).toUpperCase())}</div>
              <div class="member-name">${esc(m.username)}</div>
              ${m.username === me?.username ? '<button class="mic-btn" id="vMicBtn">🎙</button>' : ''}
            </div>`).join('')}
        </div>
        <p style="font-size:12px;color:var(--muted);margin-top:12px">WebRTC voice rooms. Используй браузер для микрофона.</p>`;

      $('vRoomCode')?.addEventListener('click', () => {
        navigator.clipboard?.writeText(room.code);
        toast('Код скопирован');
      });

      $('vBtnLeave')?.addEventListener('click', async () => {
        try { await api('/api/vizor/leave', { method: 'POST' }); } catch {}
        renderLanding();
      });
    }

    checkRoom();
  }

  /* --- PROFILE --- */
  function renderProfile() {
    const days = me?.createdAt ? Math.max(0, Math.floor((Date.now() - me.createdAt) / 86400000)) : 0;
    const dateStr = me?.createdAt ? new Date(me.createdAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' }) : '';

    app.innerHTML = `
      <div class="page">
        <div class="top-bar">
          <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
          <div class="top-actions">
            <button class="btn btn-ghost btn-sm" onclick="navigate('lobby')">← Назад</button>
          </div>
        </div>
        <div class="container">
          <div class="profile-card">
            <div class="profile-avatar">${esc((me?.username || '?').slice(0, 2).toUpperCase())}</div>
            <div class="profile-name">${esc(me?.username || '')}</div>
            <div class="profile-date">на rlua с ${esc(dateStr)}</div>
          </div>
          <div class="settings-section">
            <h2>Безопасность</h2>
            <form id="settingsForm" class="auth-form">
              <label>Текущее секретное слово</label>
              <input type="password" id="oldWord" autocomplete="current-password" required>
              <label>Текущий пароль</label>
              <input type="password" id="oldPassword" autocomplete="current-password" required>
              <label>Новое секретное слово <span style="color:var(--muted);font-weight:400">(необязательно)</span></label>
              <input type="password" id="newWord" placeholder="Минимум 6 символов" minlength="6" autocomplete="new-password">
              <label>Новый пароль <span style="color:var(--muted);font-weight:400">(необязательно)</span></label>
              <input type="password" id="newPassword" placeholder="Минимум 6 символов" minlength="6" autocomplete="new-password">
              <p class="form-error hidden" id="settingsError"></p>
              <p class="form-ok hidden" id="settingsOk">Данные обновлены</p>
              <button type="submit" class="btn btn-primary btn-block">Сохранить</button>
            </form>
          </div>
        </div>
      </div>`;

    $('settingsForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const oldWord = $('oldWord').value;
      const oldPassword = $('oldPassword').value;
      const newWord = $('newWord').value || undefined;
      const newPassword = $('newPassword').value || undefined;
      try {
        const data = await api('/api/change-password', { method: 'POST', body: JSON.stringify({ oldWord, oldPassword, newWord, newPassword }) });
        if (data.token) saveSession(me.username, data.token);
        $('settingsError').classList.add('hidden');
        $('settingsOk').classList.remove('hidden');
      } catch (err) {
        $('settingsOk').classList.add('hidden');
        $('settingsError').textContent = err.message;
        $('settingsError').classList.remove('hidden');
      }
    });
  }

  /* --- ADMIN --- */
  function renderAdmin() {
    if (me?.role !== 'admin') { navigate('lobby'); return; }

    app.innerHTML = `
      <div class="page">
        <div class="top-bar">
          <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
          <div class="top-actions">
            <button class="btn btn-ghost btn-sm" onclick="navigate('lobby')">← Назад</button>
          </div>
        </div>
        <div class="container">
          <div class="section-header"><div class="section-title">ADMIN</div></div>
          <div class="admin-bar">
            <button class="btn btn-ghost btn-sm" id="aBtnUsers">Пользователи</button>
            <button class="btn btn-ghost btn-sm" id="aBtnLogs">Логи</button>
          </div>
          <div id="adminContent"></div>
        </div>
      </div>`;

    const ac = $('adminContent');

    async function loadUsers() {
      try {
        const data = await api('/api/admin/users');
        const users = data.users || [];
        ac.innerHTML = users.map((u) => `
          <div class="admin-user-card" data-user="${esc(u.username)}">
            <div class="avatar">${esc(u.username.slice(0, 2).toUpperCase())}</div>
            <div class="info">
              <div class="uname">${esc(u.username)}</div>
              <div class="urole">${u.role} · #${u.id || '?'}</div>
            </div>
            <div class="ustatus ustatus-${u.status || 'approved'}">${u.status || 'approved'}</div>
          </div>`).join('') || '<div class="empty-state"><p>Нет пользователей</p></div>';

        ac.querySelectorAll('.admin-user-card').forEach((card) => {
          card.addEventListener('click', () => showUserDetail(card.dataset.user));
        });
      } catch (e) { toast(e.message); }
    }

    async function showUserDetail(username) {
      try {
        const data = await api('/api/admin/user/' + encodeURIComponent(username));
        const u = data.user;
        ac.innerHTML = `
          <button class="btn btn-ghost btn-sm" id="aBackUsers">← К списку</button>
          <div style="margin-top:12px">
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
              <div class="avatar">${esc(u.username.slice(0, 2).toUpperCase())}</div>
              <div>
                <div class="lobby-name">${esc(u.username)}</div>
                <div style="font-size:12px;color:var(--muted)">#${u.id} · ${u.role} · ${u.status || 'approved'}</div>
              </div>
            </div>
            <div class="admin-actions">
              ${u.status !== 'banned' ? '<button class="admin-action-btn danger" data-action="ban">Забанить</button>' : '<button class="admin-action-btn" data-action="unban">Разбанить</button>'}
              ${!u.trusted ? '<button class="admin-action-btn" data-action="trust">Доверить</button>' : '<button class="admin-action-btn" data-action="untrust">Убрать доверие</button>'}
              ${u.role !== 'admin' ? '<button class="admin-action-btn" data-action="promote">Сделать админом</button>' : ''}
              <button class="admin-action-btn danger" data-action="kick">Кикнуть</button>
              <button class="admin-action-btn danger" data-action="delete">Удалить</button>
            </div>
          </div>`;

        $('aBackUsers')?.addEventListener('click', loadUsers);

        ac.querySelectorAll('.admin-action-btn').forEach((btn) => {
          btn.addEventListener('click', async () => {
            const action = btn.dataset.action;
            if (action === 'delete' && !confirm('Удалить ' + u.username + '?')) return;
            try {
              await api('/api/admin/user/' + encodeURIComponent(username) + '/' + action, { method: 'POST' });
              toast(u.username + ': ' + action);
              loadUsers();
            } catch (e) { toast(e.message); }
          });
        });
      } catch (e) { toast(e.message); }
    }

    async function loadLogs() {
      try {
        const data = await api('/api/admin/logs');
        const logs = data.logs || [];
        ac.innerHTML = logs.slice(0, 100).map((l) => `
          <div class="log-card">
            <div class="log-type">${esc(l.type || '?')}</div>
            <div class="log-detail">${esc(l.username || '')} ${l.ip ? '· ' + esc(l.ip) : ''} ${l.details ? '· ' + esc(l.details) : ''}</div>
            <div class="log-time">${l.time ? new Date(l.time).toLocaleString('ru-RU') : ''}</div>
          </div>`).join('') || '<div class="empty-state"><p>Нет логов</p></div>';
      } catch (e) { toast(e.message); }
    }

    $('aBtnUsers')?.addEventListener('click', loadUsers);
    $('aBtnLogs')?.addEventListener('click', loadLogs);
    loadUsers();
  }

  /* --- OBF --- */
  function renderObf() {
    app.innerHTML = `
      <div class="page">
        <div class="top-bar">
          <a href="#" class="logo" onclick="event.preventDefault(); navigate('lobby')">rl<span>ua</span></a>
          <div class="top-actions">
            <button class="btn btn-ghost btn-sm" onclick="navigate('lobby')">← Назад</button>
          </div>
        </div>
        <div class="container">
          <div class="section-header"><div class="section-title">Обфускация</div></div>
          <textarea id="obfIn" style="width:100%;min-height:150px;padding:12px;border-radius:10px;border:1.5px solid var(--border);background:var(--bg);color:var(--text);font-family:var(--font-mono);font-size:13px;resize:vertical;outline:none" placeholder="Вставь Lua-код…"></textarea>
          <div style="display:flex;gap:8px;margin-top:10px;justify-content:center">
            <button class="btn btn-primary" id="obfBtn">Защитить</button>
          </div>
          <textarea id="obfOut" readonly style="width:100%;min-height:150px;padding:12px;border-radius:10px;border:1.5px solid var(--border);background:var(--bg);color:var(--text);font-family:var(--font-mono);font-size:13px;resize:vertical;outline:none;margin-top:12px" placeholder="Результат…"></textarea>
        </div>
      </div>
      <div class="wait-overlay hidden" id="obfWait"><div class="spinner"></div><div class="wait-msg" id="obfWaitMsg">Обфускация…</div></div>`;

    $('obfBtn')?.addEventListener('click', async () => {
      const code = $('obfIn')?.value?.trim();
      if (!code) { toast('Вставь код'); return; }
      $('obfWait').classList.remove('hidden');
      try {
        const data = await api('/api/obfuscate', { method: 'POST', body: JSON.stringify({ code }) });
        const job = data.job;
        let tries = 0;
        const poll = async () => {
          try {
            const s = await api('/api/obfuscate/status/' + job);
            if (s.code) { $('obfOut').value = s.code; $('obfWait').classList.add('hidden'); toast('Готово'); return; }
            if (s.error) { $('obfWait').classList.add('hidden'); toast('Ошибка: ' + s.error); return; }
            if (++tries > 120) { $('obfWait').classList.add('hidden'); toast('Таймаут'); return; }
            $('obfWaitMsg').textContent = 'Обработка… ' + (s.progress || '');
            setTimeout(poll, 5000);
          } catch (e) { $('obfWait').classList.add('hidden'); toast(e.message); }
        };
        poll();
      } catch (e) { $('obfWait').classList.add('hidden'); toast(e.message); }
    });
  }

  /* ===== Auth handlers ===== */
  window._showLogin = () => showModal('login');
  window._showReg = () => showModal('register');
  window.navigate = navigate;

  $('loginForm')?.addEventListener('submit', async (e) => {
    e.preventDefault(); hideErrors();
    const btn = $('loginForm').querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      const data = await api('/api/login', { method: 'POST', body: JSON.stringify({
        username: $('loginUsername').value.trim(),
        word: $('loginWord').value,
        password: $('loginPassword').value,
      })});
      saveSession(data.username, data.token);
      closeModal();
      $('loginForm').reset();
      await refreshAuth();
      navigate('lobby');
    } catch (err) { $('loginError').textContent = err.message; $('loginError').classList.remove('hidden'); }
    finally { btn.disabled = false; }
  });

  $('registerForm')?.addEventListener('submit', async (e) => {
    e.preventDefault(); hideErrors();
    const username = $('regUsername').value.trim();
    const word = $('regWord').value;
    const password = $('regPassword').value;
    if (!/^[a-zA-Z0-9_]{3,20}$/.test(username)) { $('registerError').textContent = 'Ник: 3–20 символов, буквы, цифры, _'; $('registerError').classList.remove('hidden'); return; }
    if (word.length < 6) { $('registerError').textContent = 'Слово ≥ 6 символов'; $('registerError').classList.remove('hidden'); return; }
    if (password.length < 6) { $('registerError').textContent = 'Пароль ≥ 6 символов'; $('registerError').classList.remove('hidden'); return; }
    const btn = $('registerForm').querySelector('button[type=submit]');
    btn.disabled = true;
    try {
      const data = await api('/api/register', { method: 'POST', body: JSON.stringify({ username, word, password }) });
      if (data.token) { saveSession(data.username, data.token); closeModal(); $('registerForm').reset(); await refreshAuth(); navigate('lobby'); return; }
      $('registerOk').textContent = data.message || 'Аккаунт создан'; $('registerOk').classList.remove('hidden');
      $('registerForm').reset();
    } catch (err) { $('registerError').textContent = err.message; $('registerError').classList.remove('hidden'); }
    finally { btn.disabled = false; }
  });

  $('modalClose')?.addEventListener('click', closeModal);
  authModal?.addEventListener('click', (e) => { if (e.target === authModal) closeModal(); });
  document.querySelectorAll('.tab').forEach((t) => t.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((x) => x.classList.toggle('active', x.dataset.tab === t.dataset.tab));
    $('loginForm').classList.toggle('hidden', t.dataset.tab !== 'login');
    $('registerForm').classList.toggle('hidden', t.dataset.tab !== 'register');
    hideErrors();
  }));

  /* ===== Bottom nav ===== */
  bottomNav.querySelectorAll('.bnav-btn').forEach((btn) => {
    btn.addEventListener('click', () => navigate(btn.dataset.page));
  });

  /* ===== SW registration ===== */
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  }

  /* ===== Init ===== */
  (async function init() {
    await refreshAuth();
    const path = location.pathname.replace(/^\//, '').split('/')[0] || 'lobby';
    navigate(path);
  })();

})();
