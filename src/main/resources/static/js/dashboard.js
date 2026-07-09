// ============================================
// TASKHERO - COMMAND CENTER CORE
// ARCHITECTURAL STANDARDS: DarS Lab v2.2
// ============================================

const API_BASE = "/api";
const HERO_AVATARS = [
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Astra" },
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Blaze" },
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Cobalt" },
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Drift" },
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Ember" },
    { url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Frost" }
];

// Metafora de icono por tipo de notificacion (evita el "todo es una estrella")
const NOTIF_ICONS = {
    TAREA_COMPLETADA:   { icon: 'bi-check-circle-fill', color: 'var(--ks-patina)' },
    NIVEL_SUBIDO:       { icon: 'bi-graph-up-arrow',    color: 'var(--ks-gold)' },
    RACHA_MANTENIDA:    { icon: 'bi-fire',              color: 'oklch(66% 0.19 40)' },
    MENSAJE_RECIBIDO:   { icon: 'bi-chat-dots-fill',    color: 'var(--ks-patina)' },
    LOGRO_DESBLOQUEADO: { icon: 'bi-award-fill',        color: 'var(--ks-gold)' },
    INVITACION_GREMIO:  { icon: 'bi-flag-fill',         color: 'var(--ks-gold)' },
    SISTEMA:            { icon: 'bi-gear-fill',         color: 'var(--ks-text-muted)' },
};
function getNotifIcon(tipo) {
    return NOTIF_ICONS[tipo] || { icon: 'bi-bell-fill', color: 'var(--ks-gold)' };
}

let allTasks = [];
let currentFilter = 'all';
let myUserId = null;
let allHeroes = [];
let myGroups = [];
let currentChatId = null;
let currentChatType = null; // 'private' | 'group'
let selectedRecruitHeroId = null;
let typingTimeout = null;
// `unreadCount` global es propiedad de websocket.js (declarado con let ahí); no redeclarar aquí.

/**
 * ARCHITECTURAL INTERCEPTOR: Centralized Session & Security Management
 */
function initSecurityContext() {
    const token = localStorage.getItem("token");

    if (!token) {
        console.warn("SISTEMA: Llave de acceso no detectada. Redirigiendo...");
        window.location.href = "index.html?reason=no_token";
        return;
    }

    // Configuración Global de AJAX (Principio DRY)
    $.ajaxSetup({
        headers: { "Authorization": `Bearer ${token}` },
        beforeSend: function(xhr) {
            console.debug(`>> TÁCTICO: Accediendo a ${this.url}`);
        },
        error: function(xhr) {
            if (xhr.status === 401 || xhr.status === 403) {
                console.error("CRÍTICO: Sesión invalidada por el servidor.");
                localStorage.clear();
                window.location.href = "index.html?reason=session_expired";
            }
        }
    });

    try {
        const decoded = parseJwt(token);
        myUserId = decoded.id;
        console.log(`ESTADO: Operando como Héroe ID ${myUserId}`);
    } catch (e) {
        localStorage.clear();
        window.location.href = "index.html?reason=token_corrupt";
    }
}

$(document).ready(function() {
    initSecurityContext();
    initNavigation();
    loadDashboard();
    renderAvatarSelector();

    // --- VIEW SWITCHER ARCHITECTURE ---
    function initNavigation() {
        $(".ks-nav-item[data-view]").click(function(e) {
            e.preventDefault();
            const viewId = $(this).data("view");
            showView(viewId);
        });
        showView('view-overview'); // Vista por defecto
    }

    function showView(viewId) {
        $(".ks-nav-item[data-view]").removeClass("active");
        $(`.ks-nav-item[data-view="${viewId}"]`).addClass("active");

        $(".ks-view").removeClass("active");
        $(`#${viewId}`).addClass("active");

        const titles = {
            'view-overview': 'Comandancia',
            'view-missions': 'Registro de Misiones',
            'view-comms': 'Enlace Táctico',
            'view-account': 'Expediente de Héroe'
        };
        $("#viewTitle").text(titles[viewId] || 'Command Center');

        if (viewId === 'view-missions') renderTasks();
        if (viewId === 'view-comms') {
            $("#chatEmptyState").removeClass("d-none");
            $("#activeChatWindow").addClass("d-none");
            closeChat();
            loadFriends();
            loadGroups();
        }
    }

    // --- FORM EVENTS ---

    $("#taskForm").submit(function(e) {
        e.preventDefault();
        const btn = $(this).find('button[type="submit"]');
        setButtonLoading(btn, true);

        const taskId = $("#taskId").val();
        const data = {
            titulo: $("#taskTitle").val(),
            descripcion: $("#taskDescription").val() || null,
            categoria: $("#taskCategory").val() || null,
            prioridad: $("#taskPriority").val(),
            fechaLimite: $("#taskDueDate").val() || null
        };

        $.ajax({
            url: taskId ? `${API_BASE}/tareas/${taskId}` : `${API_BASE}/tareas`,
            type: taskId ? "PUT" : "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function() {
                $("#taskModal").modal('hide');
                showToast(taskId ? "Misión actualizada" : "¡Misión desplegada!", "success");
                loadTasks();
            },
            error: function(xhr) {
                const fallback = taskId ? "No se pudo actualizar la misión" : "No se pudo desplegar la misión";
                showToast(xhr.responseJSON?.message || fallback, "error");
            },
            complete: () => setButtonLoading(btn, false)
        });
    });

    $("#taskModal").on("hidden.bs.modal", function() {
        $("#taskForm")[0].reset();
        $("#taskId").val('');
        $("#taskDescription").val('');
        $("#taskCategory").val('');
        $("#taskModalTitle").text("Protocolo de Nueva Misión");
        $("#taskFormSubmitBtn").text("DESPLEGAR MISIÓN");
    });

    $("#editProfileForm").submit(function(e) {
        e.preventDefault();
        const btn = $("#btnSaveProfile");
        setButtonLoading(btn, true);

        const data = {
            nombre: $("#editName").val(),
            avatarUrl: $("#editAvatarUrl").val(),
            passwordActual: $("#currentPass").val() || null,
            passwordNueva: $("#newPass").val() || null
        };

        $.ajax({
            url: `${API_BASE}/auth/profile`,
            type: "PUT",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(user) {
                showToast("Expediente sincronizado", "success");
                updateUserUI(user);
                $("#currentPass").val('');
                $("#newPass").val('');
            },
            error: function(xhr) {
                showToast(xhr.responseJSON?.message || "No se pudo sincronizar el expediente", "error");
            },
            complete: () => setButtonLoading(btn, false)
        });
    });

    // --- MISSION FILTERS ---
    $("#taskFilters button[data-filter]").click(function() {
        $("#taskFilters button").removeClass("active");
        $(this).addClass("active");
        currentFilter = $(this).data("filter");
        currentTasksPage = 1;
        renderTasks();
    });

    // --- CHAT: SEND MESSAGE ---
    $("#chatForm").submit(function(e) {
        e.preventDefault();
        const content = $("#chatInput").val().trim();
        if (!content || !currentChatId) return;

        if (currentChatType === 'group') {
            sendWSGroupMessage(currentChatId, content);
        } else {
            sendWSMessage(currentChatId, content);
        }

        // No se agrega el mensaje aqui de forma optimista: el servidor lo
        // reenvia por el mismo topico del WebSocket al que ya estamos
        // suscritos, asi que appendMessage() lo pinta cuando llegue (evita el duplicado).
        $("#chatInput").val('').trigger('input');
        sendTypingStatus(currentChatId, currentChatType === 'group', false);
    });

    $("#chatInput").on("input", function() {
        // Auto-crecer el textarea segun el contenido, como cualquier chat estandar
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';

        if (!currentChatId) return;
        sendTypingStatus(currentChatId, currentChatType === 'group', true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            sendTypingStatus(currentChatId, currentChatType === 'group', false);
        }, 1500);
    });

    // Enter envia el mensaje, Shift+Enter agrega salto de linea
    $("#chatInput").on("keydown", function(e) {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            $("#chatForm").trigger("submit");
        }
    });

    // --- GROUPS: CREATE ---
    $("#groupModal").on("show.bs.modal", renderFriendsSelectionForGroup);

    $("#groupForm").submit(function(e) {
        e.preventDefault();
        const btn = $(this).find('button[type="submit"]');
        const nombre = $("#groupName").val().trim();
        const miembrosIds = $("#friendsSelection input:checked").map(function() {
            return parseInt($(this).val());
        }).get();

        if (!nombre) return;
        setButtonLoading(btn, true);

        $.ajax({
            url: `${API_BASE}/chat/grupos`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({ nombre, miembrosIds }),
            success: function() {
                $("#groupModal").modal('hide');
                $("#groupForm")[0].reset();
                showToast("¡Gremio fundado!", "success");
                loadGroups();
            },
            error: function(xhr) {
                showToast(xhr.responseJSON?.message || "No se pudo fundar el gremio", "error");
            },
            complete: () => setButtonLoading(btn, false)
        });
    });

    // --- RECRUIT: SEARCH & INVITE ---
    $("#recruitModal").on("show.bs.modal", function() {
        $("#heroSearchInput").val('');
        renderHeroSearchResults(allHeroes);
    });

    $("#heroSearchInput").on("input", function() {
        const q = $(this).val().toLowerCase();
        renderHeroSearchResults(allHeroes.filter(h => h.nombre.toLowerCase().includes(q)));
    });

    $("#selectGuildModal").on("show.bs.modal", renderGuildSelectionForInvite);

    // --- SOCIAL & LOGS ---
    $("#btnShowLogros").click(() => { $("#achievementsModal").modal('show'); loadAchievements(); });
    $("#btnMarkAllRead").click(markAllNotificationsRead);

    $("#logoutBtn").click(() => {
        showToast("Finalizando enlace seguro...", "info");
        setTimeout(() => {
            localStorage.clear();
            window.location.href = "index.html";
        }, 600);
    });
});

// --- CARGA INICIAL ---

function loadDashboard() {
    loadTasks();
    syncProfile();
    loadNotifications();
}

function syncProfile() {
    $.get(`${API_BASE}/auth/profile`, function(user) {
        updateUserUI(user);
    });
}

function updateUserUI(user) {
    if (!user) return;

    $("#displayUserName").text(user.nombre);
    const avatar = user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.nombre)}&background=random`;
    $("#userAvatar, #profileAvatar").attr("src", avatar);

    $("#profileName").text(user.nombre);
    $("#profileEmail").text(user.email);
    $("#editName").val(user.nombre);
    $("#editAvatarUrl").val(user.avatarUrl || "");

    const nivel = user.nivelActual || 1;
    const targetXp = nivel < 5 ? 500 : 2000;
    const percent = Math.min(Math.round((user.xpTotal / targetXp) * 100), 100);

    $("#sidebarLevel").text(nivel);
    $("#sidebarLevelName").text(getRankName(nivel));
    $("#sidebarXpBar").css("width", percent + "%");
    $("#sidebarXpPercent").text(percent + "%");
    $("#sidebarCurrentXp").text(`${user.xpTotal} XP`);

    if (nivel < 5) {
        $("#chatLocked").removeClass("d-none");
        $("#chatSection").addClass("d-none");
    } else {
        $("#chatLocked").addClass("d-none");
        $("#chatSection").removeClass("d-none");
    }
}

function getRankName(level) {
    if (level >= 10) return "Leyenda";
    if (level >= 8) return "Maestro";
    if (level >= 5) return "Comandante";
    return "Aprendiz";
}

function loadTasks() {
    $.ajax({
        url: `${API_BASE}/tareas`,
        type: "GET",
        success: function(tasks) {
            allTasks = tasks;
            syncTimerClientStarts();
            renderTasks();
            renderActiveTimerBar();
            updateStats();
            renderOverviewCharts();
        }
    });
}

function updateStats() {
    $("#statTotal").text(allTasks.length);
    $("#statCompleted").text(allTasks.filter(t => t.completada).length);
    $("#statPending").text(allTasks.filter(t => !t.completada).length);
}

// ============================================
// TABLERO: gráficos y parte de situación (calculados en el cliente, sin endpoints nuevos)
// ============================================

function renderOverviewCharts() {
    renderPriorityChart();
    renderCategoryChart();
    renderMentorAdvice();
}

function todayISO() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Barra fina, extremos redondeados, etiqueta directa (nombre + valor) en vez de tooltip:
// con 3-6 barras como mucho, el valor siempre visible es mas claro que exigir hover.
function chartBarRow(label, value, max, color) {
    const pct = value > 0 ? Math.max(4, Math.round((value / max) * 100)) : 0;
    return `
        <div style="margin-bottom: 0.9rem;">
            <div class="ks-mono" style="display: flex; justify-content: space-between; font-size: 0.65rem; color: var(--ks-text-muted); margin-bottom: 0.35rem;">
                <span>${escapeHtml(label)}</span><span style="color: var(--ks-text-warm); font-weight: 600;">${value}</span>
            </div>
            <div style="height: 8px; background: var(--ks-graphite-2); border-radius: 4px; overflow: hidden;">
                <div style="height: 100%; width: ${pct}%; background: ${color}; border-radius: 4px; transition: width 0.6s var(--ease-out);"></div>
            </div>
        </div>
    `;
}

// Identidad (prioridad) -> color fijo por categoria, reutilizando el mismo mapeo que
// los puntos de prioridad en la lista de misiones (nunca se reordena por ranking).
function renderPriorityChart() {
    const counts = { BAJA: 0, MEDIA: 0, ALTA: 0 };
    allTasks.forEach(t => { if (t.prioridad in counts) counts[t.prioridad]++; });
    const colors = { BAJA: 'var(--ks-text-muted)', MEDIA: 'var(--ks-gold)', ALTA: 'oklch(58% 0.15 35)' };
    const max = Math.max(1, counts.BAJA, counts.MEDIA, counts.ALTA);

    const html = ['BAJA', 'MEDIA', 'ALTA']
        .map(key => chartBarRow(key, counts[key], max, colors[key]))
        .join('');
    $("#priorityChart").html(html);
}

// Magnitud (conteo por categoria) -> un solo hue (dorado), ordenado de mayor a menor;
// se limita a las 5 categorias mas usadas para no repetir el anti-patron de lista larga.
function renderCategoryChart() {
    const counts = {};
    allTasks.forEach(t => {
        const cat = (t.categoria && t.categoria.trim()) ? t.categoria.trim().toUpperCase() : 'GENERAL';
        counts[cat] = (counts[cat] || 0) + 1;
    });

    const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 5);
    if (entries.length === 0) {
        $("#categoryChart").html('<div class="ks-mono opacity-50 small text-center py-4">Sin misiones aún</div>');
        return;
    }

    const max = Math.max(1, ...entries.map(e => e[1]));
    const html = entries.map(([label, value]) => chartBarRow(label, value, max, 'var(--ks-gold)')).join('');
    $("#categoryChart").html(html);
}

// Reemplaza el texto estatico "Analizando..." por un resumen real calculado sobre
// allTasks: prioriza avisar atrasos, luego vencimientos de hoy, luego el estado general.
function renderMentorAdvice() {
    const hoy = todayISO();
    const pendientes = allTasks.filter(t => !t.completada);
    const atrasadas = pendientes.filter(t => t.fechaLimite && t.fechaLimite < hoy);
    const hoyVencen = pendientes.filter(t => t.fechaLimite && t.fechaLimite === hoy);

    let mensaje;
    if (atrasadas.length > 0) {
        mensaje = `Tienes ${atrasadas.length} misión${atrasadas.length > 1 ? 'es' : ''} atrasada${atrasadas.length > 1 ? 's' : ''}. Es momento de retomar el mando.`;
    } else if (hoyVencen.length > 0) {
        mensaje = `${hoyVencen.length} misión${hoyVencen.length > 1 ? 'es' : ''} vence${hoyVencen.length > 1 ? 'n' : ''} hoy. Complétala${hoyVencen.length > 1 ? 's' : ''} para no perder la racha.`;
    } else if (pendientes.length === 0) {
        mensaje = "Tablero despejado, Comandante. Inicia una nueva misión cuando estés listo.";
    } else {
        mensaje = `${pendientes.length} misión${pendientes.length > 1 ? 'es' : ''} en curso, sin vencimientos próximos. Vas bien.`;
    }
    $("#mentorAdvice").text(mensaje);
}

const TASKS_PER_PAGE = 6;
let currentTasksPage = 1;

// Cronometros por tarea: un solo heartbeat global (independiente de la vista activa y de los
// re-renders) evita que cambiar de vista "reinicie" el contador visualmente. `timerClientStart`
// guarda, por tarea, el Date.now() del momento en que sincronizamos su base con el servidor;
// solo se toca cuando el servidor confirma un cambio real (fetch/iniciar/pausar), nunca al re-pintar.
const timerClientStart = {};
let globalTimerHeartbeat = null;

function syncTimerClientStarts() {
    const activeIds = new Set();
    allTasks.forEach(t => {
        if (t.timerActivo) {
            activeIds.add(t.id);
            // Cada fetch trae tiempoInvertidoSegundos ya recalculado a este instante por el
            // servidor, asi que el punto de partida local SIEMPRE debe reiniciarse a ahora;
            // no hacerlo (solo la primera vez) duplicaba el tramo ya incluido en la nueva base.
            timerClientStart[t.id] = Date.now();
        }
    });
    Object.keys(timerClientStart).forEach(id => {
        if (!activeIds.has(Number(id))) delete timerClientStart[id];
    });
    ensureGlobalTimerHeartbeat();
}

function ensureGlobalTimerHeartbeat() {
    if (globalTimerHeartbeat) return;
    globalTimerHeartbeat = setInterval(() => {
        renderTimerDisplays();
        renderActiveTimerBar();
    }, 1000);
}

function computeLiveSeconds(task) {
    if (!task.timerActivo) return task.tiempoInvertidoSegundos;
    const start = timerClientStart[task.id] || Date.now();
    return task.tiempoInvertidoSegundos + Math.floor((Date.now() - start) / 1000);
}

// Solo actualiza el texto de los cronometros ya pintados (si la vista Misiones esta activa);
// no reconstruye el DOM, asi que no interfiere con paginacion/filtros en curso.
function renderTimerDisplays() {
    allTasks.filter(t => t.timerActivo).forEach(t => {
        $(`.task-timer-display[data-task-id="${t.id}"]`).text(formatSeconds(computeLiveSeconds(t)));
    });
}

function renderActiveTimerBar() {
    const bar = $("#activeTimerBar").empty();
    const activos = allTasks.filter(t => t.timerActivo);
    activos.forEach(t => {
        bar.append(`
            <div class="ks-card" style="display: flex; align-items: center; gap: 0.6rem; padding: 0.5rem 0.9rem; border: 1px solid var(--ks-gold-hairline);">
                <i class="bi bi-stopwatch-fill timer-pulse-dot" style="color: var(--ks-patina);"></i>
                <span class="ks-mono" style="font-size: 0.65rem; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ks-text-muted);">${escapeHtml(t.titulo)}</span>
                <span class="ks-mono header-timer-display" data-task-id="${t.id}" style="font-size: 0.8rem; font-weight: 600; color: var(--ks-gold);">${formatSeconds(computeLiveSeconds(t))}</span>
                <button class="ks-btn ks-btn-timer-active" style="font-size: 0.6rem; padding: 0.3rem 0.6rem;" onclick="toggleTaskTimer(${t.id}, this)"><i class="bi bi-pause-fill"></i></button>
            </div>
        `);
    });
}

function formatSeconds(totalSeconds) {
    const s = Math.max(0, Math.floor(totalSeconds));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    const pad = n => String(n).padStart(2, '0');
    return h > 0 ? `${pad(h)}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`;
}

// Estado del timer -> boton claro segun UX: Iniciar Tarea / Pausar (en curso) / Reanudar
function renderTimerControl(task) {
    let btnClass, icon, label;
    if (task.timerActivo) {
        btnClass = 'ks-btn-timer-active';
        icon = 'bi-pause-fill';
        label = 'PAUSAR';
    } else if (task.tiempoInvertidoSegundos > 0) {
        btnClass = 'ks-btn-secondary';
        icon = 'bi-play-fill';
        label = 'REANUDAR';
    } else {
        btnClass = 'ks-btn-primary';
        icon = 'bi-play-fill';
        label = 'INICIAR TAREA';
    }

    return `
        <div style="display: flex; align-items: center; gap: 0.85rem; margin-top: 0.75rem;">
            <button class="ks-btn ${btnClass}" style="font-size: 0.7rem; padding: 0.45rem 1rem;" onclick="toggleTaskTimer(${task.id}, this)">
                <i class="bi ${icon}"></i> ${label}
            </button>
            <span class="ks-mono task-timer-display" data-task-id="${task.id}" style="font-size: 0.8rem; color: var(--ks-gold); font-weight: 600;">${formatSeconds(computeLiveSeconds(task))}</span>
            ${task.timerActivo ? `<span class="ks-mono timer-pulse-dot" style="font-size: 0.6rem; color: var(--ks-patina); display: inline-flex; align-items: center; gap: 0.3rem;"><i class="bi bi-circle-fill" style="font-size: 0.4rem;"></i> EN CURSO</span>` : ''}
        </div>
    `;
}

function renderTasks() {
    const container = $("#tasksContainer").empty();
    const pagination = $("#tasksPagination").empty();
    const filtered = allTasks.filter(t => {
        return currentFilter === 'all' || (currentFilter === 'pending' ? !t.completada : t.completada);
    });

    if (allTasks.length === 0) {
        $("#emptyState").removeClass("d-none");
        return;
    }
    $("#emptyState").addClass("d-none");

    const totalPages = Math.max(1, Math.ceil(filtered.length / TASKS_PER_PAGE));
    if (currentTasksPage > totalPages) currentTasksPage = totalPages;
    const start = (currentTasksPage - 1) * TASKS_PER_PAGE;
    const pageItems = filtered.slice(start, start + TASKS_PER_PAGE);

    pageItems.forEach((task, index) => {
        const pClass = task.prioridad === 'ALTA' ? 'pr-high' : (task.prioridad === 'MEDIA' ? 'pr-med' : 'pr-low');
        const row = $(`
            <div class="task-row ks-enter ${task.completada ? 'completed' : ''}" style="animation-delay: ${index * 0.04}s">
                <div style="display: flex; align-items: flex-start; gap: 1.5rem; min-width: 0; flex: 1;">
                    <div class="priority-mark ${pClass}" style="flex-shrink: 0; margin-top: 0.3rem;"></div>
                    <div style="min-width: 0; overflow-wrap: anywhere;">
                        <div class="${task.completada ? 'task-title-completed' : ''}" style="font-weight: 600;">
                            ${escapeHtml(task.titulo)}
                        </div>
                        ${task.descripcion ? `<div class="ks-mono" style="font-size: 0.7rem; color: var(--ks-text-muted); margin-top: 0.4rem; white-space: pre-wrap; overflow-wrap: anywhere;">${escapeHtml(task.descripcion)}</div>` : ''}
                        <div class="ks-mono" style="font-size: 0.6rem; color: var(--ks-text-faint); margin-top: 0.4rem;">
                            ${task.prioridad}${task.categoria ? ` · ${escapeHtml(task.categoria).toUpperCase()}` : ''}
                        </div>
                        ${!task.completada ? renderTimerControl(task) : `
                        <div class="ks-mono" style="font-size: 0.65rem; color: var(--ks-text-faint); margin-top: 0.6rem;">
                            <i class="bi bi-stopwatch"></i> ${formatSeconds(task.tiempoInvertidoSegundos)} invertidos
                        </div>`}
                    </div>
                </div>
                <div style="display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0;">
                    ${!task.completada ? `<button class="ks-btn ks-btn-secondary" style="font-size: 0.7rem;" onclick="completeTask(${task.id}, this)">COMPLETAR</button>` : `<span class="ks-mono text-patina" style="display: inline-flex; align-items: center; gap: 0.4rem;"><i class="bi bi-check-circle-fill"></i>LOGRADA</span>`}
                    <button class="ks-btn ks-btn-secondary" title="Editar" style="font-size: 0.7rem; padding: 0.5rem 0.7rem;" onclick="editTask(${task.id})"><i class="bi bi-pencil-fill"></i></button>
                    <button class="ks-btn ks-btn-secondary" title="Eliminar" style="font-size: 0.7rem; padding: 0.5rem 0.7rem;" onclick="deleteTask(${task.id}, this)"><i class="bi bi-trash-fill"></i></button>
                </div>
            </div>
        `);
        container.append(row);
    });

    if (totalPages > 1) renderTasksPagination(pagination, totalPages);
}

function toggleTaskTimer(id, btn) {
    const task = allTasks.find(t => t.id === id);
    if (!task) return;
    const action = task.timerActivo ? 'pausar' : 'iniciar';

    $(btn).prop("disabled", true);
    $.ajax({
        url: `${API_BASE}/tareas/${id}/timer/${action}`,
        type: "PATCH",
        success: function(updated) {
            const idx = allTasks.findIndex(t => t.id === id);
            if (idx !== -1) allTasks[idx] = updated;

            if (updated.timerActivo) {
                timerClientStart[updated.id] = Date.now();
            } else {
                delete timerClientStart[updated.id];
            }
            ensureGlobalTimerHeartbeat();

            renderTasks();
            renderActiveTimerBar();
        },
        error: function() {
            showToast(`No se pudo ${action} el cronómetro`, "error");
        },
        complete: () => $(btn).prop("disabled", false)
    });
}

function renderTasksPagination(pagination, totalPages) {
    const prev = $(`<button class="ks-btn ks-btn-secondary" style="padding: 0.5rem 1rem;" ${currentTasksPage === 1 ? 'disabled' : ''}><i class="bi bi-chevron-left"></i></button>`);
    prev.click(() => { currentTasksPage--; renderTasks(); });
    pagination.append(prev);

    pagination.append(`<span class="ks-mono" style="font-size: 0.7rem; color: var(--ks-text-muted);">PÁGINA ${currentTasksPage} DE ${totalPages}</span>`);

    const next = $(`<button class="ks-btn ks-btn-secondary" style="padding: 0.5rem 1rem;" ${currentTasksPage === totalPages ? 'disabled' : ''}><i class="bi bi-chevron-right"></i></button>`);
    next.click(() => { currentTasksPage++; renderTasks(); });
    pagination.append(next);
}

function openNewTaskModal() {
    $("#taskForm")[0].reset();
    $("#taskId").val('');
    $("#taskModalTitle").text("Protocolo de Nueva Misión");
    $("#taskFormSubmitBtn").text("DESPLEGAR MISIÓN");
    $("#taskModal").modal('show');
}

function editTask(id) {
    const task = allTasks.find(t => t.id === id);
    if (!task) return;

    $("#taskId").val(task.id);
    $("#taskTitle").val(task.titulo);
    $("#taskDescription").val(task.descripcion || '');
    $("#taskCategory").val(task.categoria || '');
    $("#taskPriority").val(task.prioridad);
    $("#taskDueDate").val(task.fechaLimite || '');
    $("#taskModalTitle").text("Editar Misión");
    $("#taskFormSubmitBtn").text("GUARDAR CAMBIOS");
    $("#taskModal").modal('show');
}

function deleteTask(id, btn) {
    showConfirm("¿Abandonar esta misión permanentemente? Esta acción no se puede deshacer.", function() {
        $(btn).prop("disabled", true);
        $.ajax({
            url: `${API_BASE}/tareas/${id}`,
            type: "DELETE",
            success: function() {
                showToast("Misión eliminada", "success");
                loadDashboard();
            },
            error: function() {
                $(btn).prop("disabled", false);
                showToast("No se pudo eliminar la misión", "error");
            }
        });
    });
}

// Dialogo Si/No reutilizable (reemplaza confirm() nativo para mantener la estetica de la app)
function showConfirm(message, onConfirm) {
    $("#confirmModalMessage").text(message);
    $("#confirmModalYesBtn").off("click").on("click", function() {
        $("#confirmModal").modal('hide');
        onConfirm();
    });
    $("#confirmModal").modal('show');
}

function completeTask(id, btn) {
    $(btn).prop("disabled", true).text("...");
    $.ajax({
        url: `${API_BASE}/tareas/${id}/completar`,
        type: "PATCH",
        success: function() {
            showToast("Misión cumplida", "success");
            loadDashboard();
        },
        error: function() {
            $(btn).prop("disabled", false).text("COMPLETAR");
            showToast("No se pudo completar la misión", "error");
        }
    });
}

// --- NOTIFICACIONES ---

function loadNotifications() {
    $.get(`${API_BASE}/notificaciones`, function(notifs) {
        const list = $("#notificationList").empty();
        unreadCount = notifs.filter(n => !n.leida).length;

        if (unreadCount > 0) {
            $("#notifBadge").text(unreadCount).removeClass("d-none");
        } else {
            $("#notifBadge").addClass("d-none");
        }

        if (notifs.length === 0) {
            list.append('<li class="py-4 small text-center" style="color: var(--ks-text-faint);"><i class="bi bi-inbox"></i> Sin notificaciones</li>');
            return;
        }

        // Pila (LIFO): la más reciente siempre arriba, sin asumir el orden que devuelve el backend.
        notifs.slice()
            .sort((a, b) => new Date(b.fechaCreacion) - new Date(a.fechaCreacion))
            .forEach(n => list.append(buildNotificationItem(n)));
    });
}

function buildNotificationItem(notif) {
    let actionBtn = '';
    let mensaje = notif.mensaje;
    if (notif.tipo === 'INVITACION_GREMIO') {
        const match = mensaje.match(/\[GRP:(\d+)\]/);
        if (match) {
            mensaje = mensaje.replace(/\[GRP:\d+\]/, '');
            actionBtn = `<button class="btn btn-sm btn-success mt-2 py-0 px-2 fw-bold" onclick="acceptGuildInvite(${match[1]}, ${notif.id})">Unirse</button>`;
        }
    }
    const readBtn = notif.leida ? '' : `<button type="button" class="notif-mark-read" title="Marcar como leída" onclick="markNotificationRead(${notif.id}, this)"><i class="bi bi-check-lg"></i></button>`;
    const { icon, color } = getNotifIcon(notif.tipo);
    return `
        <li class="notif-item ${notif.leida ? '' : 'unread'}" data-notif-id="${notif.id}">
            <i class="bi ${icon}" style="color: ${color}; margin-top: 0.15rem;"></i>
            <div style="flex: 1;">
                <div class="notif-msg small">${escapeHtml(mensaje)}</div>
                ${actionBtn}
            </div>
            ${readBtn}
        </li>
    `;
}

function markNotificationRead(notifId, btn) {
    $.ajax({
        url: `${API_BASE}/notificaciones/${notifId}/leer`,
        type: "PATCH",
        success: function() {
            const item = $(btn).closest('.notif-item');
            item.removeClass('unread');
            $(btn).remove();
            unreadCount = Math.max(0, unreadCount - 1);
            if (unreadCount > 0) $("#notifBadge").text(unreadCount);
            else $("#notifBadge").addClass("d-none");
        }
    });
}

function markAllNotificationsRead() {
    $("#notificationList .notif-item.unread").each(function() {
        const notifId = $(this).data("notif-id");
        $.ajax({ url: `${API_BASE}/notificaciones/${notifId}/leer`, type: "PATCH" });
    });
    $("#notificationList .notif-item").removeClass("unread").find(".notif-mark-read").remove();
    unreadCount = 0;
    $("#notifBadge").addClass("d-none");
}

function acceptGuildInvite(grupoId, notifId) {
    $.ajax({
        url: `${API_BASE}/chat/grupos/${grupoId}/aceptar`,
        type: "POST",
        success: function() {
            showToast("¡Te uniste al gremio!", "success");
            $.ajax({ url: `${API_BASE}/notificaciones/${notifId}/leer`, type: "PATCH" });
            loadNotifications();
            loadGroups();
        },
        error: function() {
            showToast("No se pudo aceptar la invitación", "error");
        }
    });
}

// --- LOGROS ---
function loadAchievements() {
    const container = $("#achievementsList");
    container.html('<div class="col-12 text-center opacity-50 py-4"><i class="bi bi-hourglass-split fs-2 d-block mb-2"></i>Cargando...</div>');

    $.ajax({
        url: `${API_BASE}/logros`,
        type: "GET",
        success: function(logros) {
            container.empty();
            logros.forEach(l => container.append(buildAchievementCard(l)));
        },
        error: function() {
            container.html('<div class="col-12 text-center opacity-50 py-4">No se pudo cargar la sala de trofeos</div>');
        }
    });
}

function buildAchievementCard(logro) {
    const opacity = logro.desbloqueado ? '1' : '0.35';
    const fecha = logro.fechaObtenido ? new Date(logro.fechaObtenido).toLocaleDateString() : '';
    return `
        <div class="col-6 col-md-4">
            <div class="ks-card text-center" style="padding: 1.25rem 0.75rem; opacity: ${opacity}; border: 1px solid ${logro.desbloqueado ? 'var(--ks-gold-hairline)' : 'var(--ks-rule)'};">
                <i class="bi ${logro.icono || 'bi-award-fill'}" style="font-size: 1.8rem; color: ${logro.desbloqueado ? 'var(--ks-gold)' : 'var(--ks-text-faint)'};"></i>
                <div class="ks-mono" style="font-size: 0.7rem; font-weight: 700; margin-top: 0.6rem; color: var(--ks-champagne);">${escapeHtml(logro.nombre)}</div>
                <div class="ks-mono" style="font-size: 0.6rem; color: var(--ks-text-muted); margin-top: 0.3rem;">${escapeHtml(logro.descripcion)}</div>
                ${logro.desbloqueado ? `<div class="ks-mono text-patina" style="font-size: 0.55rem; margin-top: 0.5rem;"><i class="bi bi-check-circle-fill"></i> ${fecha}</div>` : `<div class="ks-mono" style="font-size: 0.55rem; margin-top: 0.5rem; color: var(--ks-text-faint);"><i class="bi bi-lock-fill"></i> BLOQUEADO</div>`}
            </div>
        </div>
    `;
}

// --- UTILIDADES ---
function showToast(msg, type) {
    const toast = $("#liveToast");
    $("#toastMessage").text(msg);
    toast.css("border-color", type === 'success' ? 'var(--ks-patina)' : 'var(--ks-warning)');
    new bootstrap.Toast(toast[0]).show();
}

function setButtonLoading(btn, loading) {
    if (loading) btn.prop("disabled", true).css("opacity", "0.6");
    else btn.prop("disabled", false).css("opacity", "1");
}

function renderAvatarSelector() {
    const container = $("#avatarSelector").empty();
    HERO_AVATARS.forEach(avatar => {
        const el = $(`<img src="${avatar.url}" class="avatar-option" style="width:40px; cursor:pointer; border-radius:4px; border: 2px solid transparent;">`);
        el.click(function() {
            $(".avatar-option").css("border-color", "transparent");
            $(this).css("border-color", "var(--ks-gold)");
            $("#editAvatarUrl").val(avatar.url);
        });
        container.append(el);
    });
}

function parseJwt(token) { try { return JSON.parse(atob(token.split('.')[1])); } catch (e) { return {}; } }
function escapeHtml(text) { if (!text) return ''; const div = document.createElement('div'); div.textContent = text; return div.innerHTML; }

// ============================================
// AMIGOS Y GRUPOS
// ============================================

function loadFriends() {
    $.ajax({
        url: `${API_BASE}/auth/heroes`,
        type: "GET",
        success: function(heroes) {
            allHeroes = heroes;
            const list = $("#friendsList").empty();
            if (heroes.length === 0) return list.append('<div class="ks-mono opacity-50 py-3 small">SIN ALIADOS</div>');

            heroes.forEach(f => {
                list.append(`
                    <div class="ks-nav-item" style="padding: 0.5rem 0;" onclick="openChat(${f.id}, '${escapeHtml(f.nombre)}')">
                        <div style="width: 8px; height: 8px; background: var(--ks-patina); border-radius: 50%; opacity: 0.4;"></div>
                        <span class="ks-mono" style="font-size: 0.7rem;">${escapeHtml(f.nombre)}</span>
                    </div>
                `);
            });
        }
    });
}

function loadGroups() {
    $.ajax({
        url: `${API_BASE}/chat/grupos`,
        type: "GET",
        success: function(grupos) {
            myGroups = grupos;
            const list = $("#groupsList").empty();
            if (grupos.length === 0) return list.append('<div class="ks-mono opacity-50 py-3 small">SIN GREMIOS</div>');

            grupos.forEach(g => {
                list.append(`
                    <div class="ks-nav-item" style="padding: 0.5rem 0;" onclick="openGroupChat(${g.id}, '${escapeHtml(g.nombre)}')">
                        <i class="bi bi-collection-fill" style="font-size: 0.7rem;"></i>
                        <span class="ks-mono" style="font-size: 0.7rem;">${escapeHtml(g.nombre)}</span>
                    </div>
                `);
            });
        }
    });
}

function renderFriendsSelectionForGroup() {
    const container = $("#friendsSelection").empty();
    if (allHeroes.length === 0) {
        container.append('<div class="ks-mono opacity-50 small">SIN ALIADOS DISPONIBLES</div>');
        return;
    }
    allHeroes.forEach(h => {
        container.append(`
            <label class="d-flex align-items-center gap-2 py-1" style="cursor:pointer;">
                <input type="checkbox" value="${h.id}"> <span class="ks-mono" style="font-size: 0.75rem;">${escapeHtml(h.nombre)}</span>
            </label>
        `);
    });
}

function renderHeroSearchResults(heroes) {
    const container = $("#heroSearchResults").empty();
    if (heroes.length === 0) {
        container.append('<div class="ks-mono opacity-50 small py-3 text-center">SIN RESULTADOS</div>');
        return;
    }
    heroes.forEach(h => {
        container.append(`
            <div class="list-group-item d-flex justify-content-between align-items-center" style="background: transparent;">
                <span class="ks-mono" style="font-size: 0.75rem;">${escapeHtml(h.nombre)}</span>
                <button class="ks-btn ks-btn-secondary" style="font-size: 0.65rem; padding: 0.4rem 0.8rem;" onclick="startRecruit(${h.id})">INVITAR</button>
            </div>
        `);
    });
}

function startRecruit(heroId) {
    selectedRecruitHeroId = heroId;
    $("#recruitModal").modal('hide');
    $("#selectGuildModal").modal('show');
}

function renderGuildSelectionForInvite() {
    const container = $("#guildSelectionList").empty();
    if (myGroups.length === 0) {
        container.append('<div class="ks-mono opacity-50 small p-3 text-center">FUNDA UN GREMIO PRIMERO</div>');
        return;
    }
    myGroups.forEach(g => {
        container.append(`
            <button type="button" class="list-group-item list-group-item-action" style="background: transparent;" onclick="inviteToGuild(${g.id})">
                <span class="ks-mono" style="font-size: 0.75rem;">${escapeHtml(g.nombre)}</span>
            </button>
        `);
    });
}

function inviteToGuild(grupoId) {
    if (!selectedRecruitHeroId) return;
    $.ajax({
        url: `${API_BASE}/chat/grupos/${grupoId}/invitar/${selectedRecruitHeroId}`,
        type: "POST",
        success: function() {
            $("#selectGuildModal").modal('hide');
            showToast("Invitación enviada", "success");
            selectedRecruitHeroId = null;
        },
        error: function(xhr) {
            showToast(xhr.responseJSON?.message || "No se pudo invitar", "error");
        }
    });
}

// ============================================
// CHAT ARCHITECTURE
// ============================================

function openChat(id, nombre) {
    currentChatId = id;
    currentChatType = 'private';
    $("#chatEmptyState").addClass("d-none");
    $("#activeChatWindow").removeClass("d-none");
    $("#activeChatTarget").text(nombre);
    $("#chatMessages").html('<div class="ks-mono opacity-50 text-center py-5">INICIANDO ENLACE...</div>');

    $.ajax({
        url: `${API_BASE}/chat/privado/${id}`,
        type: "GET",
        success: function(messages) {
            $("#chatMessages").empty();
            if (messages.length === 0) {
                $("#chatMessages").html('<div class="ks-mono opacity-50 text-center py-5">SIN MENSAJES. ¡INICIA LA CONVERSACIÓN!</div>');
                return;
            }
            messages.forEach(msg => appendMessage(msg));
            $("#chatMessages").scrollTop($("#chatMessages")[0].scrollHeight);
        }
    });
}

function openGroupChat(id, nombre) {
    currentChatId = id;
    currentChatType = 'group';
    $("#chatEmptyState").addClass("d-none");
    $("#activeChatWindow").removeClass("d-none");
    $("#activeChatTarget").text(nombre);
    $("#chatMessages").html('<div class="ks-mono opacity-50 text-center py-5">CONECTANDO A FRECUENCIA...</div>');

    $.ajax({
        url: `${API_BASE}/chat/grupo/${id}`,
        type: "GET",
        success: function(messages) {
            $("#chatMessages").empty();
            if (messages.length === 0) {
                $("#chatMessages").html('<div class="ks-mono opacity-50 text-center py-5">SIN MENSAJES. ¡INICIA LA CONVERSACIÓN!</div>');
                return;
            }
            messages.forEach(msg => appendMessage(msg));
            $("#chatMessages").scrollTop($("#chatMessages")[0].scrollHeight);
        }
    });
}

function closeChat() {
    currentChatId = null;
    currentChatType = null;
    $("#activeChatWindow").addClass("d-none");
    $("#chatEmptyState").removeClass("d-none");
}

function updateTypingIndicator(data) {
    if (!currentChatId) return;
    $("#activeChatTarget").siblings("small.typing-hint").remove();
    if (data.escribiendo) {
        $("#activeChatTarget").after('<small class="typing-hint ks-mono opacity-50" style="margin-left: 0.5rem; font-size: 0.6rem;">escribiendo...</small>');
    }
}
