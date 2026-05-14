const API_BASE = "http://localhost:8080/api";
const token = localStorage.getItem("token");
let allTasks = [];
let currentFilter = 'all';
let currentSearch = '';
let myUserId = null;

if (!token) {
    window.location.href = "index.html";
} else {
    myUserId = parseJwt(token).id;
}

$(document).ready(function() {
    initTheme();
    loadDashboard();

    // Eventos de UI
    $("#logoutBtn").click(() => { localStorage.clear(); window.location.href = "index.html"; });
    
    $("#taskSearch").on("input", function() {
        currentSearch = $(this).val().toLowerCase();
        renderTasks();
    });

    $("#taskFilters button").click(function() {
        $("#taskFilters button").removeClass("btn-dark active").addClass("btn-outline-secondary");
        $(this).removeClass("btn-outline-secondary").addClass("btn-dark active");
        currentFilter = $(this).data("filter");
        renderTasks();
    });

    $("#themeToggle").change(function() {
        toggleDarkMode($(this).is(":checked"));
    });

    $("#markAllRead").click(function(e) {
        e.preventDefault();
        $("#notificationList").html('<li class="p-3 text-center text-muted small">No hay noticias</li>');
        $("#notifBadge").text("0").addClass("d-none");
        if (typeof unreadCount !== 'undefined') unreadCount = 0;
    });

    $("#taskForm").submit(function(e) {
        e.preventDefault();
        const data = {
            titulo: $("#taskTitle").val(),
            prioridad: $("#taskPriority").val(),
            fechaLimite: $("#taskDueDate").val() || null
        };
        $.ajax({
            url: `${API_BASE}/tareas`,
            type: "POST",
            headers: { "Authorization": `Bearer ${token}` },
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function() {
                $("#taskModal").modal('hide');
                $("#taskForm")[0].reset();
                loadTasks();
            }
        });
    });

    // Chat Submit con Soporte Grupal
    $("#chatForm").off("submit").submit(function(e) {
        e.preventDefault();
        const content = $("#chatInput").val();
        if (!content) return;
        
        if (currentChatId && typeof sendWSMessage === 'function') {
            sendWSMessage(currentChatId, content);
            $("#chatInput").val("");
            sendTypingStatus(currentChatId, false, false);
        } else if (currentGroupId && typeof sendWSGroupMessage === 'function') {
            sendWSGroupMessage(currentGroupId, content);
            $("#chatInput").val("");
            sendTypingStatus(currentGroupId, true, false);
        }
    });

    // Escribiendo... Evento
    let typingTimer;
    $("#chatInput").on("input", function() {
        const targetId = currentChatId || currentGroupId;
        const isGroup = !!currentGroupId;
        if (!targetId) return;

        sendTypingStatus(targetId, isGroup, true);
        
        clearTimeout(typingTimer);
        typingTimer = setTimeout(() => {
            sendTypingStatus(targetId, isGroup, false);
        }, 3000);
    });
});

function initTheme() {
    const savedTheme = localStorage.getItem("theme") || "light";
    document.documentElement.setAttribute("data-theme", savedTheme);
    $("#themeToggle").prop("checked", savedTheme === "dark");
}

function toggleDarkMode(isDark) {
    const theme = isDark ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("theme", theme);
}

function loadDashboard() {
    loadTasks();
    checkLevelAndUnlockFeatures();
}

function checkLevelAndUnlockFeatures() {
    $.ajax({
        url: `${API_BASE}/auth/profile`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(user) {
            $("#displayUserName, #profileName").text(user.nombre);
            $("#profileEmail").text(user.email);
            $("#levelBadge").text(`Nivel ${user.nivelActual}`);
            $("#currentXp").text(user.xpTotal);
            $("#rachaValue").text(user.rachaDias);
            $("#userAvatar, #profileAvatar").attr("src", `https://ui-avatars.com/api/?name=${user.nombre}&background=random`);
            
            let levelTitle = "Aprendiz de Software";
            if (user.nivelActual >= 10) levelTitle = "Leyenda del Código";
            else if (user.nivelActual >= 5) levelTitle = "Comandante de Proyectos";
            else if (user.nivelActual >= 3) levelTitle = "Héroe Novato";
            $("#levelName").text(levelTitle);

            const progress = Math.min((user.xpTotal % 200) / 2, 100);
            $("#xpBar").css("width", `${progress}%`);

            if (user.nivelActual >= 5) {
                $("#chatSection").removeClass("d-none");
                $("#chatLocked").addClass("d-none");
                loadFriends();
                loadGroups();
            }
        }
    });
}

function loadTasks() {
    $.ajax({
        url: `${API_BASE}/tareas`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(tasks) {
            allTasks = tasks;
            renderTasks();
        }
    });
}

function renderTasks() {
    const container = $("#tasksContainer");
    container.empty();
    const filtered = allTasks.filter(t => {
        const matchesFilter = currentFilter === 'all' || (currentFilter === 'pending' ? !t.completada : t.completada);
        const matchesSearch = t.titulo.toLowerCase().includes(currentSearch);
        return matchesFilter && matchesSearch;
    });
    $("#pendingCount").text(allTasks.filter(t => !t.completada).length);
    $("#tasksDone").text(allTasks.filter(t => t.completada).length);
    if (filtered.length === 0) {
        container.append('<div class="col-12 text-center text-muted py-5">No se encontraron misiones</div>');
        return;
    }
    filtered.forEach(task => {
        const priorityClass = task.prioridad === 'ALTA' ? 'bg-danger' : (task.prioridad === 'BAJA' ? 'bg-success' : 'bg-warning');
        container.append(`
            <div class="col-md-6">
                <div class="card h-100 shadow-sm border-0 ${task.completada ? 'opacity-75' : ''}">
                    <div class="card-body">
                        <div class="d-flex justify-content-between mb-2">
                            <span class="badge ${priorityClass} small">${task.prioridad}</span>
                            <small class="text-muted">${task.fechaLimite || 'Eterna'}</small>
                        </div>
                        <h6 class="fw-bold ${task.completada ? 'text-decoration-line-through text-muted' : ''}">${task.titulo}</h6>
                        ${!task.completada ? `<button class="btn btn-sm btn-primary w-100 mt-2 rounded-pill" onclick="completeTask(${task.id})">Vencer Misión</button>` : '<div class="text-success small mt-2 fw-bold">Misión Cumplida</div>'}
                    </div>
                </div>
            </div>
        `);
    });
}

function completeTask(id) {
    $.ajax({
        url: `${API_BASE}/tareas/${id}/completar`,
        type: "PATCH",
        headers: { "Authorization": `Bearer ${token}` },
        success: function() { loadTasks(); }
    });
}

function loadFriends() {
    $.ajax({
        url: `${API_BASE}/auth/heroes`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(heroes) {
            const list = $("#friendsList");
            const selection = $("#friendsSelection");
            list.empty(); selection.empty();
            heroes.forEach(f => {
                list.append(`<button class="list-group-item list-group-item-action border-0 py-2 d-flex justify-content-between align-items-center" onclick="openChat(${f.id}, '${f.nombre}')"><span>${f.nombre}</span></button>`);
                selection.append(`<label class="list-group-item d-flex gap-2 border-0"><input class="form-check-input" type="checkbox" name="invitedFriends" value="${f.id}"><span>${f.nombre}</span></label>`);
            });
        }
    });
}

function loadGroups() {
    $.ajax({
        url: `${API_BASE}/chat/grupos`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(grupos) {
            const list = $("#groupsList");
            list.empty();
            if (grupos.length === 0) list.append('<small class="text-muted ps-3 py-2 d-block">Sin gremios</small>');
            grupos.forEach(g => {
                list.append(`<button class="list-group-item list-group-item-action border-0 py-2" onclick="openGroupChat(${g.id}, '${g.nombre}')"><span>${g.nombre}</span></button>`);
            });
        }
    });
}

$("#groupForm").submit(function(e) {
    e.preventDefault();
    const miembrosIds = [];
    $("input[name='invitedFriends']:checked").each(function() { miembrosIds.push(parseInt($(this).val())); });
    const data = { nombre: $("#groupName").val(), miembrosIds: miembrosIds };
    $.ajax({
        url: `${API_BASE}/chat/grupos`,
        type: "POST",
        headers: { "Authorization": `Bearer ${token}` },
        contentType: "application/json",
        data: JSON.stringify(data),
        success: function() { $("#groupModal").modal('hide'); $("#groupForm")[0].reset(); loadGroups(); if (typeof fetchGruposAndSubscribe === 'function') fetchGruposAndSubscribe(); }
    });
});

let currentChatId = null;
let currentGroupId = null;

function openChat(id, nombre) {
    currentChatId = id; currentGroupId = null;
    $("#activeChatWindow").removeClass("d-none");
    $("#activeChatTarget").text(nombre);
    $("#chatMessages").empty();
    $.ajax({
        url: `${API_BASE}/chat/privado/${id}`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(messages) { messages.forEach(msg => appendMessage(msg)); }
    });
}

function openGroupChat(id, nombre) {
    currentGroupId = id; currentChatId = null;
    $("#activeChatWindow").removeClass("d-none");
    $("#activeChatTarget").text(nombre);
    $("#chatMessages").empty();
    $.ajax({
        url: `${API_BASE}/chat/grupo/${id}`,
        type: "GET",
        headers: { "Authorization": `Bearer ${token}` },
        success: function(messages) { messages.forEach(msg => appendMessage(msg)); }
    });
}

function closeChat() { $("#activeChatWindow").addClass("d-none"); currentChatId = null; currentGroupId = null; }

function appendMessage(msg) {
    if (currentChatId && (msg.remitenteId != currentChatId && msg.remitenteId != myUserId)) return;
    if (currentGroupId && msg.grupoId != currentGroupId) return;

    const isMe = msg.remitenteId == myUserId;
    const align = isMe ? 'ms-auto text-end' : 'me-auto';
    const bubbleClass = isMe ? 'message-bubble-me' : 'message-bubble-other';
    const time = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    const senderInfo = isMe ? '' : `<small class="d-block text-muted mb-1 fw-bold" style="font-size: 0.75rem; color: var(--hero-primary) !important;">${msg.remitenteNombre || 'Héroe Desconocido'}</small>`;

    $("#chatMessages").append(`
        <div class="mb-3 ${align}" style="max-width: 85%;">
            ${senderInfo}
            <div class="${bubbleClass}" style="display: inline-block;">
                ${msg.contenido}
            </div>
            <small class="d-block text-muted mt-1" style="font-size: 0.65rem;">${time}</small>
        </div>
    `);
    $("#chatMessages").scrollTop($("#chatMessages")[0].scrollHeight);
}

function updateTypingIndicator(data) {
    const activeId = currentChatId || currentGroupId;
    const isActiveGroup = !!currentGroupId;
    const relevant = isActiveGroup ? (data.grupoId == activeId) : (data.remitenteId == activeId);

    if (relevant && data.escribiendo) {
        $("#typingIndicator").removeClass("d-none");
        $("#typingUser").text(data.remitenteNombre + " está escribiendo...");
    } else {
        $("#typingIndicator").addClass("d-none");
    }
}

function parseJwt(token) { try { return JSON.parse(atob(token.split('.')[1])); } catch (e) { return {}; } }
