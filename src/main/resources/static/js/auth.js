$(document).ready(function() {
    const API_URL = "http://localhost:8080/api/auth";

    // Toggle forms
    $("#showRegister").click(() => {
        $("#loginForm").addClass("d-none");
        $("#registerForm").removeClass("d-none");
        $("#showRegister").parent().addClass("d-none");
    });

    $("#showLogin").click(() => {
        $("#registerForm").addClass("d-none");
        $("#loginForm").removeClass("d-none");
        $("#showRegister").parent().removeClass("d-none");
    });

    // Handle Login
    $("#loginForm").submit(function(e) {
        e.preventDefault();
        const data = {
            email: $("#loginEmail").val(),
            password: $("#loginPassword").val()
        };

        $.ajax({
            url: `${API_URL}/login`,
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(response) {
                localStorage.setItem("token", response.token);
                localStorage.setItem("userName", response.nombre);
                window.location.href = "dashboard.html";
            },
            error: function(xhr) {
                alert("Error: " + (xhr.responseJSON ? xhr.responseJSON.message : "Credenciales inválidas"));
            }
        });
    });

    // Handle Register
    $("#registerForm").submit(function(e) {
        e.preventDefault();
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
                alert("Héroe creado con éxito. ¡Ahora inicia sesión!");
                $("#showLogin").click();
            },
            error: function(xhr) {
                alert("Error: " + (xhr.responseJSON ? xhr.responseJSON.message : "Error al registrar"));
            }
        });
    });
});
