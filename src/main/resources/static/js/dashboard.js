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

        const data = {
            titulo: $("#taskTitle").val(),
            prioridad: $("#taskPriority").val(),
            fechaLimite: $("#taskDueDate").val() || null
        };

        $.ajax({
            url: `${API_BASE}/tareas`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function() {
                $("#taskModal").modal('hide');
                $("#taskForm")[0].reset();
                showToast("¡Misión desplegada!", "success");
                loadTasks();
            },
            error: function() {
                showToast("No se pudo desplegar la misión", "error");
            },
            complete: () => setButtonLoading(btn, false)
        });
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

        if (typeof appendMessage === 'function') {
            appendMessage({
                remitenteId: myUserId,
                remitenteNombre: 'Yo',
                contenido: content,
                timestamp: new Date().toISOString()
            });
        }
        $("#chatInput").val('');
        sendTypingStatus(currentChatId, currentChatType === 'group', false);
    });

    $("#chatInput").on("input", function() {
        if (!currentChatId) return;
        sendTypingStatus(currentChatId, currentChatType === 'group', true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            sendTypingStatus(currentChatId, currentChatType === 'group', false);
        }, 1500);
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
    $("#btnShowEventos").click(() => showToast("No hay eventos tácticos programados", "info"));

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
            renderTasks();
            updateStats();
        }
    });
}

function updateStats() {
    $("#statTotal").text(allTasks.length);
    $("#statCompleted").text(allTasks.filter(t => t.completada).length);
    $("#statPending").text(allTasks.filter(t => !t.completada).length);
}

const TASKS_PER_PAGE = 6;
let currentTasksPage = 1;

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
            <div class="task-row ks-enter" style="animation-delay: ${index * 0.04}s">
                <div style="display: flex; align-items: center; gap: 1.5rem;">
                    <div class="priority-mark ${pClass}"></div>
                    <div>
                        <div class="${task.completada ? 'text-muted' : ''}" style="font-weight: 600; ${task.completada ? 'text-decoration: line-through' : ''}">
                            ${escapeHtml(task.titulo)}
                        </div>
                        <div class="ks-mono" style="font-size: 0.6rem; color: var(--ks-text-faint); margin-top: 0.25rem;">
                            ${task.prioridad}
                        </div>
                    </div>
                </div>
                <div>
                    ${!task.completada ? `<button class="ks-btn ks-btn-secondary" style="font-size: 0.7rem;" onclick="completeTask(${task.id}, this)">COMPLETAR</button>` : `<span class="ks-mono text-patina">LOGRADA</span>`}
                </div>
            </div>
        `);
        container.append(row);
    });

    if (totalPages > 1) renderTasksPagination(pagination, totalPages);
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
    return `
        <li class="notif-item ${notif.leida ? '' : 'unread'}" data-notif-id="${notif.id}">
            <i class="bi bi-stars" style="color: var(--ks-gold); margin-top: 0.15rem;"></i>
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

// --- LOGROS (sin backend aún) ---
function loadAchievements() {
    // ponytail: no existe endpoint de logros en el backend todavía; se muestra estado vacío en vez de spinner infinito.
    $("#achievementsList").html('<div class="col-12 text-center opacity-50 py-4"><i class="bi bi-hourglass-split fs-2 d-block mb-2"></i>Sistema de logros en desarrollo</div>');
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
