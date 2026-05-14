let stompClient = null;
let unreadCount = 0;

$(document).ready(function() {
    initChatSystem();
});

function initChatSystem() {
    const token = localStorage.getItem("token");
    if (!token) return;

    // Usar el myUserId ya declarado en dashboard.js
    console.log("Sistema de Chat iniciado para el Héroe ID:", myUserId);

    connect();
}

function connect() {
    const token = localStorage.getItem("token");
    
    stompClient = new StompJs.Client({
        brokerURL: `ws://${window.location.host}/ws`,
        connectHeaders: {
            Authorization: `Bearer ${token}`
        },
        debug: function (str) {
            console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
    });

    stompClient.webSocketFactory = function () {
        return new SockJS("/ws");
    };

    stompClient.onConnect = (frame) => {
        console.log('¡Sincronizado con el Gremio de Héroes!');
        
        // 1. Suscribirse a NOTIFICACIONES
        stompClient.subscribe('/topic/notifications/' + myUserId, function(message) {
            handleIncomingNotification(JSON.parse(message.body));
        });

        // 2. Suscribirse a CHAT privado
        stompClient.subscribe('/topic/chat/private/' + myUserId, function(message) {
            console.log("Nuevo mensaje recibido vía Socket");
            const msg = JSON.parse(message.body);
            
            // Si el mensaje es para mí y no soy el remitente, mostrar alerta/notificación
            if (msg.receptorId === myUserId && msg.remitenteId !== myUserId) {
                showChatAlert("Nuevo mensaje de un Héroe", msg.contenido);
            }

            if (typeof appendMessage === 'function') {
                appendMessage(msg);
            }
        });

        // 2.1 Suscribirse a ESTADO ESCRIBIENDO privado
        stompClient.subscribe('/topic/chat/private/' + myUserId + '/typing', function(message) {
            const data = JSON.parse(message.body);
            handleTypingEvent(data);
        });

        // 3. Suscribirse a CHATS de GRUPO (Dinámico)
        fetchGruposAndSubscribe();
    };

    stompClient.activate();
}

function fetchGruposAndSubscribe() {
    const token = localStorage.getItem("token");
    fetch("/api/chat/grupos", {
        headers: { "Authorization": "Bearer " + token }
    })
    .then(r => r.json())
    .then(grupos => {
        grupos.forEach(grupo => {
            // Mensajes del grupo
            stompClient.subscribe('/topic/chat/group/' + grupo.id, function(message) {
                const msg = JSON.parse(message.body);
                if (msg.remitenteId !== myUserId) {
                    showChatAlert("Mensaje en grupo: " + grupo.nombre, msg.contenido);
                }
                if (typeof appendMessage === 'function') {
                    appendMessage(msg);
                }
            });

            // Escribiendo en el grupo
            stompClient.subscribe('/topic/chat/group/' + grupo.id + '/typing', function(message) {
                const data = JSON.parse(message.body);
                handleTypingEvent(data);
            });
        });
    });
}

function handleTypingEvent(data) {
    // Si soy yo el que escribe (porque el servidor me rebotó el mensaje en grupos), ignorar
    if (data.remitenteId == myUserId) return;

    // Solo mostrar si el chat está abierto
    if (typeof updateTypingIndicator === 'function') {
        updateTypingIndicator(data);
    }
}

function sendTypingStatus(targetId, isGroup, isTyping) {
    if (stompClient && stompClient.connected) {
        const payload = {
            escribiendo: isTyping
        };
        if (isGroup) payload.grupoId = parseInt(targetId);
        else payload.receptorId = parseInt(targetId);

        stompClient.publish({
            destination: "/app/chat.typing",
            body: JSON.stringify(payload)
        });
    }
}

function showChatAlert(title, content) {
    // Configurar icono y título para mensajes de chat
    const toastHeader = $("#liveToast .toast-header");
    toastHeader.find("i").removeClass().addClass("bi bi-chat-dots-fill me-2");
    toastHeader.find("strong").text("Mensaje del Gremio");

    $("#toastMessage").html(`<strong>${title}</strong><br>${content}`);
    const toast = new bootstrap.Toast(document.getElementById('liveToast'));
    toast.show();
}

function sendWSMessage(targetId, content) {
    if (stompClient && stompClient.connected) {
        const payload = {
            receptorId: parseInt(targetId),
            contenido: content
        };
        stompClient.publish({
            destination: "/app/chat.sendPrivate",
            body: JSON.stringify(payload)
        });
        console.log("Mensaje privado enviado...");
    } else {
        alert("La conexión con el gremio se ha perdido. Reconectando...");
        connect();
    }
}

function sendWSGroupMessage(groupId, content) {
    if (stompClient && stompClient.connected) {
        const payload = {
            grupoId: parseInt(groupId),
            contenido: content
        };
        stompClient.publish({
            destination: "/app/chat.sendGroup",
            body: JSON.stringify(payload)
        });
        console.log("Mensaje grupal enviado...");
    } else {
        alert("La conexión con el gremio se ha perdido. Reconectando...");
        connect();
    }
}

function handleIncomingNotification(notif) {
    // Restaurar icono original para notificaciones de sistema (Héroe/Misión)
    const toastHeader = $("#liveToast .toast-header");
    toastHeader.find("i").removeClass().addClass("bi bi-check-circle-fill me-2");
    toastHeader.find("strong").text("TaskHero");

    $("#toastMessage").text(notif.mensaje);
    const toast = new bootstrap.Toast(document.getElementById('liveToast'));
    toast.show();

    unreadCount++;
    $("#notifBadge").text(unreadCount).removeClass("d-none");

    const list = $("#notificationList");
    if (list.find(".small").length > 0) list.empty();
    
    list.prepend(`
        <li class="list-group-item list-group-item-action border-0 py-2 small">
            <i class="bi bi-stars text-primary me-2"></i>${notif.mensaje}
        </li>
    `);

    if (typeof checkLevelAndUnlockFeatures === 'function') {
        checkLevelAndUnlockFeatures();
    }
}
