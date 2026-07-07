// ============================================
// TASKHERO - AUTHENTICATION CORE
// ============================================

$(document).ready(function() {
    const API_URL = "/api/auth";

    // --- FORM TOGGLES ---
    $("#showRegister").click(() => {
        $("#loginForm").addClass("d-none");
        $("#registerForm").removeClass("d-none");
        $("#loginToggleText").addClass("d-none");
        $("#registerToggleText").removeClass("d-none");
    });

    $("#showLogin").click(() => {
        $("#registerForm").addClass("d-none");
        $("#loginForm").removeClass("d-none");
        $("#registerToggleText").addClass("d-none");
        $("#loginToggleText").removeClass("d-none");
    });

    // --- LOGIN ---
    $("#loginForm").submit(function(e) {
        e.preventDefault();
        const btn = $("#loginBtn");
        setBtnLoading(btn, true);

        const data = {
            email: $("#loginEmail").val(),
            password: $("#loginPassword").val()
        };

        $.ajax({
            url: `${API_URL}/login`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(res) {
                localStorage.setItem("token", res.token);
                window.location.href = "dashboard.html";
            },
            error: function(xhr) {
                const msg = xhr.responseJSON?.message || "Acceso denegado";
                showAuthToast(msg, "error");
            },
            complete: () => setBtnLoading(btn, false)
        });
    });

    // --- REGISTER ---
    $("#registerForm").submit(function(e) {
        e.preventDefault();
        const btn = $("#registerBtn");
        setBtnLoading(btn, true);

        const data = {
            nombre: $("#regNombre").val(),
            email: $("#regEmail").val(),
            password: $("#regPassword").val()
        };

        $.ajax({
            url: `${API_URL}/register`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function() {
                showAuthToast("Cuenta fundada. Ya puede sincronizar.", "success");
                $("#showLogin").click();
            },
            error: function(xhr) {
                const msg = xhr.responseJSON?.message || "Error en el registro";
                showAuthToast(msg, "error");
            },
            complete: () => setBtnLoading(btn, false)
        });
    });

    // --- STRENGTH METER ---
    $('#regPassword').on('input', function() {
        const pass = $(this).val();
        const bar = $('#strengthBar');
        let strength = 0;
        if (pass.length >= 6) strength++;
        if (/[A-Z]/.test(pass)) strength++;
        if (/[0-9]/.test(pass)) strength++;
        
        bar.css('width', (strength * 33.3) + '%');
    });
});

function showAuthToast(msg, type) {
    const toast = $("#liveToast");
    $("#toastMessage").text(msg);
    toast.css("border-color", type === 'success' ? 'var(--ks-patina)' : 'var(--ks-warning)');
    new bootstrap.Toast(toast[0]).show();
}

function setBtnLoading(btn, loading) {
    if (loading) btn.prop("disabled", true).css("opacity", "0.6").text("...");
    else btn.prop("disabled", false).css("opacity", "1").text(btn.data("original") || "SINCRONIZAR");
}
